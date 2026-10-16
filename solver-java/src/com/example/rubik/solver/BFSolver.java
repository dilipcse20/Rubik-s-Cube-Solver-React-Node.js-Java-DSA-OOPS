package com.example.rubik.solver;

import com.example.rubik.model.Cube;
import com.example.rubik.model.Move;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Breadth-First Search: explore all states 1 move away, then all states
 * 2 moves away, then 3, and so on.
 *
 * Because it finishes each depth before starting the next, the first time it
 * reaches the solved cube it has found a shortest solution.
 * The price is memory: it must remember every state it has seen.
 */
public class BFSolver implements Solver {

    @Override
    public String getName() {
        return "BFS";
    }

    @Override
    public SolverResult solve(Cube start, int maxDepth, long maxStates) {
        if (start.isSolved()) {
            return SolverResult.solved(List.of(), 0);
        }

        long stateLimit = Math.min(maxStates, MEMORY_STATE_CAP);
        Set<String> visited = new HashSet<>();
        Queue<Node> queue = new ArrayDeque<>();
        long explored = 0;

        visited.add(start.key());
        queue.add(new Node(start, null, null, 0));

        try {
            while (!queue.isEmpty()) {
                Node current = queue.poll();
                if (current.depth >= maxDepth) {
                    continue;
                }

                for (Move move : Move.ALL) {
                    Cube next = current.cube.applied(move);
                    if (!visited.add(next.key())) {
                        continue;
                    }

                    explored++;
                    Node child = new Node(next, current, move, current.depth + 1);
                    if (next.isSolved()) {
                        return SolverResult.solved(buildPath(child), explored);
                    }
                    if (explored >= stateLimit) {
                        return SolverResult.limitReached(explored);
                    }
                    queue.add(child);
                }
            }
        } catch (OutOfMemoryError error) {
            // Free the memory first, then report the limit honestly.
            queue = null;
            visited = null;
            return SolverResult.limitReached(explored);
        }

        return SolverResult.notFound(explored);
    }

    /** Walks from the solved node back to the start, collecting the moves. */
    private List<Move> buildPath(Node goal) {
        List<Move> path = new ArrayList<>();
        Node current = goal;
        while (current.parent != null) {
            path.add(current.move);
            current = current.parent;
        }
        Collections.reverse(path);
        return path;
    }

    /** One entry in the search tree: a cube, how we got to it, and how deep it is. */
    private static final class Node {
        private final Cube cube;
        private final Node parent;
        private final Move move;
        private final int depth;

        private Node(Cube cube, Node parent, Move move, int depth) {
            this.cube = cube;
            this.parent = parent;
            this.move = move;
            this.depth = depth;
        }
    }
}
