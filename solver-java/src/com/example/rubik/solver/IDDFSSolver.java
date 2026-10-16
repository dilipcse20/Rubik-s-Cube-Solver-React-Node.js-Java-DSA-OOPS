package com.example.rubik.solver;

import com.example.rubik.model.Cube;
import com.example.rubik.model.Move;

import java.util.ArrayList;
import java.util.List;

/**
 * Iterative Deepening DFS: run depth-limited DFS with limit 1, then 2, then 3...
 *
 * It gets the shortest-solution guarantee of BFS (every move costs 1, and
 * shallower depths are always finished first) but only needs memory for the
 * current path, not for every state. The price is time: shallow levels are
 * searched again and again, which is cheap because the last level is by far
 * the biggest.
 */
public class IDDFSSolver implements Solver {

    @Override
    public String getName() {
        return "IDDFS";
    }

    @Override
    public SolverResult solve(Cube start, int maxDepth, long maxStates) {
        if (start.isSolved()) {
            return SolverResult.solved(List.of(), 0);
        }

        long totalExplored = 0;
        for (int limit = 1; limit <= maxDepth; limit++) {
            DepthLimitedSearch search = new DepthLimitedSearch(maxStates - totalExplored);
            boolean found = search.explore(start, limit, null);
            totalExplored += search.explored;

            if (found) {
                return SolverResult.solved(search.path, totalExplored);
            }
            if (search.limitHit) {
                return SolverResult.limitReached(totalExplored);
            }
        }
        return SolverResult.notFound(totalExplored);
    }

    /** One pass of depth-limited DFS with no visited set (so memory stays tiny). */
    private static final class DepthLimitedSearch {
        private final long stateLimit;
        private final List<Move> path = new ArrayList<>();
        private long explored = 0;
        private boolean limitHit = false;

        private DepthLimitedSearch(long stateLimit) {
            this.stateLimit = stateLimit;
        }

        private boolean explore(Cube cube, int remaining, Move previous) {
            if (remaining == 0) {
                return cube.isSolved();
            }

            for (Move move : Move.ALL) {
                // Turning the same face twice in a row is never needed in a shortest solution.
                if (move.sameFace(previous)) {
                    continue;
                }
                if (explored >= stateLimit) {
                    limitHit = true;
                    return false;
                }

                explored++;
                path.add(move);
                if (explore(cube.applied(move), remaining - 1, move)) {
                    return true;
                }
                path.remove(path.size() - 1);
                if (limitHit) {
                    return false;
                }
            }
            return false;
        }
    }
}
