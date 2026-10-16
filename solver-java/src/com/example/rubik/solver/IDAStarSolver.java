package com.example.rubik.solver;

import com.example.rubik.model.Cube;
import com.example.rubik.model.CubeGeometry;
import com.example.rubik.model.Move;

import java.util.ArrayList;
import java.util.List;

/**
 * Iterative Deepening A*.
 *
 * For every state n we compute:
 *   g(n) = moves made so far
 *   h(n) = an estimate of the moves still needed (the heuristic)
 *   f(n) = g(n) + h(n)
 * A state is pruned (not explored further) when f(n) is larger than the
 * current threshold. If no solution is found, the threshold grows to the
 * smallest f value that was pruned, and we search again.
 *
 * Our heuristic: one move touches only 4 corner pieces and 4 edge pieces.
 * So if C corners are not home we need at least ceil(C / 4) moves, and if
 * E edges are not home we need at least ceil(E / 4) moves.
 * h = the larger of the two. It never over-estimates, so it is admissible
 * and the first solution found is still a shortest one.
 *
 * HONEST NOTE: this heuristic is weak (it is never bigger than 3). It prunes
 * a lot near the end of a search, so IDA* is much faster than IDDFS on shallow
 * scrambles, but it cannot rescue deep ones. Strong cube solvers use pattern
 * databases, which are not part of this beginner version.
 */
public class IDAStarSolver implements Solver {

    private static final int FOUND = -1;
    private static final int PIECES_MOVED_PER_TURN = 4;

    @Override
    public String getName() {
        return "IDA*";
    }

    @Override
    public SolverResult solve(Cube start, int maxDepth, long maxStates) {
        if (start.isSolved()) {
            return SolverResult.solved(List.of(), 0);
        }

        long totalExplored = 0;
        int threshold = heuristic(start);

        while (threshold <= maxDepth) {
            Search search = new Search(threshold, maxStates - totalExplored);
            int result = search.explore(start, 0, null);
            totalExplored += search.explored;

            if (result == FOUND) {
                return SolverResult.solved(search.path, totalExplored);
            }
            if (search.limitHit) {
                return SolverResult.limitReached(totalExplored);
            }
            threshold = result;
        }
        return SolverResult.notFound(totalExplored);
    }

    /** Admissible estimate of the moves still needed. Public so tests can check it. */
    public int heuristic(Cube cube) {
        int wrongCorners = countWrongPieces(cube, CubeGeometry.cornerStickers());
        int wrongEdges = countWrongPieces(cube, CubeGeometry.edgeStickers());
        return Math.max(roundUpDivide(wrongCorners), roundUpDivide(wrongEdges));
    }

    private int roundUpDivide(int wrongPieces) {
        return (wrongPieces + PIECES_MOVED_PER_TURN - 1) / PIECES_MOVED_PER_TURN;
    }

    /** A piece is "wrong" if any of its stickers differs from the solved cube. */
    private int countWrongPieces(Cube cube, int[][] pieces) {
        int wrong = 0;
        for (int[] piece : pieces) {
            if (!isPieceHome(cube, piece)) {
                wrong++;
            }
        }
        return wrong;
    }

    private boolean isPieceHome(Cube cube, int[] piece) {
        for (int stickerIndex : piece) {
            if (cube.stickerAt(stickerIndex) != Cube.solvedStickerAt(stickerIndex)) {
                return false;
            }
        }
        return true;
    }

    /** One threshold-limited search pass. */
    private final class Search {
        private final int threshold;
        private final long stateLimit;
        private final List<Move> path = new ArrayList<>();
        private long explored = 0;
        private boolean limitHit = false;

        private Search(int threshold, long stateLimit) {
            this.threshold = threshold;
            this.stateLimit = stateLimit;
        }

        /**
         * Returns FOUND, or the smallest f value that went over the threshold
         * (which becomes the next threshold).
         */
        private int explore(Cube cube, int g, Move previous) {
            int f = g + heuristic(cube);
            if (f > threshold) {
                return f;
            }
            if (cube.isSolved()) {
                return FOUND;
            }

            int smallestExceeding = Integer.MAX_VALUE;
            for (Move move : Move.ALL) {
                // Do not undo or merge with the move we just made.
                if (move.sameFace(previous)) {
                    continue;
                }
                if (explored >= stateLimit) {
                    limitHit = true;
                    return Integer.MAX_VALUE;
                }

                explored++;
                path.add(move);
                int result = explore(cube.applied(move), g + 1, move);
                if (result == FOUND) {
                    return FOUND;
                }
                path.remove(path.size() - 1);
                if (limitHit) {
                    return Integer.MAX_VALUE;
                }
                smallestExceeding = Math.min(smallestExceeding, result);
            }
            return smallestExceeding;
        }
    }
}
