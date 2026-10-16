package com.example.rubik.solver;

import com.example.rubik.model.Move;

import java.util.Collections;
import java.util.List;

/** What a solver returns: the outcome, the moves (if any) and how much work it did. */
public final class SolverResult {

    public enum Status {
        /** A solution was found (it may be empty if the cube was already solved). */
        SOLVED,
        /** The whole search space within the depth limit was searched: no solution that short. */
        NOT_FOUND_WITHIN_DEPTH,
        /** The search stopped early because the state limit (or memory) ran out. */
        STATE_LIMIT_REACHED
    }

    private final Status status;
    private final List<Move> moves;
    private final long statesExplored;

    private SolverResult(Status status, List<Move> moves, long statesExplored) {
        this.status = status;
        this.moves = Collections.unmodifiableList(moves);
        this.statesExplored = statesExplored;
    }

    public static SolverResult solved(List<Move> moves, long statesExplored) {
        return new SolverResult(Status.SOLVED, moves, statesExplored);
    }

    public static SolverResult notFound(long statesExplored) {
        return new SolverResult(Status.NOT_FOUND_WITHIN_DEPTH, List.of(), statesExplored);
    }

    public static SolverResult limitReached(long statesExplored) {
        return new SolverResult(Status.STATE_LIMIT_REACHED, List.of(), statesExplored);
    }

    public Status getStatus() {
        return status;
    }

    public boolean isSolved() {
        return status == Status.SOLVED;
    }

    public List<Move> getMoves() {
        return moves;
    }

    public long getStatesExplored() {
        return statesExplored;
    }
}
