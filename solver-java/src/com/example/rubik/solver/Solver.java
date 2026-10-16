package com.example.rubik.solver;

import com.example.rubik.model.Cube;

/**
 * Common contract for every solving algorithm.
 *
 * The service only talks to this interface, so it does not care which
 * algorithm is behind it. That is polymorphism: BFSolver, DFSolver,
 * IDDFSSolver and IDAStarSolver can all be used the same way.
 */
public interface Solver {

    /**
     * Algorithms that remember visited states keep them in memory, so they
     * never store more than this many. Keeps a beginner laptop safe.
     */
    long MEMORY_STATE_CAP = 500_000;

    /** Short name shown to the user, for example "IDDFS". */
    String getName();

    /**
     * Searches for a move sequence that solves the cube.
     *
     * @param start     the cube to solve
     * @param maxDepth  the longest solution (in moves) the search may return
     * @param maxStates the most states the search may generate before giving up
     */
    SolverResult solve(Cube start, int maxDepth, long maxStates);
}
