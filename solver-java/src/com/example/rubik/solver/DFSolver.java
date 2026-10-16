package com.example.rubik.solver;

import com.example.rubik.model.Cube;
import com.example.rubik.model.Move;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Depth-limited Depth-First Search: follow one branch as deep as allowed,
 * then back up and try the next branch.
 *
 * Without a depth limit DFS could walk forever, so we stop at maxDepth.
 * We also remember the shallowest depth at which each state was seen, so
 * we never repeat work on a state we already reached at the same or lower depth.
 *
 * DFS returns the FIRST solution it stumbles on, not the shortest one.
 */
public class DFSolver implements Solver {

    @Override
    public String getName() {
        return "DFS";
    }

    @Override
    public SolverResult solve(Cube start, int maxDepth, long maxStates) {
        if (start.isSolved()) {
            return SolverResult.solved(List.of(), 0);
        }

        long stateLimit = Math.min(maxStates, MEMORY_STATE_CAP);
        Search search = new Search(maxDepth, stateLimit);

        try {
            search.visited.put(start.key(), 0);
            if (search.explore(start, 0, null)) {
                return SolverResult.solved(search.path, search.explored);
            }
        } catch (OutOfMemoryError error) {
            search.visited = null;
            return SolverResult.limitReached(search.explored);
        }

        if (search.limitHit) {
            return SolverResult.limitReached(search.explored);
        }
        return SolverResult.notFound(search.explored);
    }

    /** Holds the data that changes while one search runs. */
    private static final class Search {
        private final int maxDepth;
        private final long stateLimit;
        private Map<String, Integer> visited = new HashMap<>();
        private final List<Move> path = new ArrayList<>();
        private long explored = 0;
        private boolean limitHit = false;

        private Search(int maxDepth, long stateLimit) {
            this.maxDepth = maxDepth;
            this.stateLimit = stateLimit;
        }

        private boolean explore(Cube cube, int depth, Move previous) {
            if (depth == maxDepth) {
                return false;
            }

            for (Move move : Move.ALL) {
                if (move.sameFace(previous)) {
                    continue;
                }
                if (explored >= stateLimit) {
                    limitHit = true;
                    return false;
                }

                Cube next = cube.applied(move);
                String key = next.key();
                Integer seenAtDepth = visited.get(key);
                if (seenAtDepth != null && seenAtDepth <= depth + 1) {
                    continue;
                }

                explored++;
                visited.put(key, depth + 1);
                path.add(move);

                if (next.isSolved() || explore(next, depth + 1, move)) {
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
