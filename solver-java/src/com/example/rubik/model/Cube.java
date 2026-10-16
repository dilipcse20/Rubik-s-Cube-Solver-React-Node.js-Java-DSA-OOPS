package com.example.rubik.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * A 3x3x3 Rubik's Cube stored as 54 stickers.
 *
 * Each sticker is a character: the letter of the face that sticker belongs to
 * in the solved cube (so 'U' means "the colour of the Up face").
 *
 * A Cube never changes after it is created. Applying a move returns a NEW
 * Cube. This makes search algorithms safe, because going back (backtracking)
 * just means using the old Cube again.
 */
public final class Cube {

    private static final String FACE_LETTERS = "UDFBLR";
    private static final char[] SOLVED_STICKERS = buildSolvedStickers();

    private final char[] stickers;

    private Cube(char[] stickers) {
        this.stickers = stickers;
    }

    public static Cube solved() {
        return new Cube(SOLVED_STICKERS.clone());
    }

    public static Cube fromState(CubeState state) {
        return new Cube(state.asString().toCharArray());
    }

    public Cube copy() {
        return new Cube(stickers.clone());
    }

    /** Applies one move and returns the resulting cube. */
    public Cube applied(Move move) {
        int[] destination = CubeGeometry.destination(move.getFace(), move.getTurns());
        char[] result = new char[CubeGeometry.STICKER_COUNT];

        for (int i = 0; i < stickers.length; i++) {
            result[destination[i]] = stickers[i];
        }
        return new Cube(result);
    }

    /** Applies several moves in order. */
    public Cube applied(List<Move> moves) {
        Cube current = this;
        for (Move move : moves) {
            current = current.applied(move);
        }
        return current;
    }

    public boolean isSolved() {
        return Arrays.equals(stickers, SOLVED_STICKERS);
    }

    /** A unique text for this state. Two cubes are equal exactly when their keys are equal. */
    public String key() {
        return new String(stickers);
    }

    public CubeState toState() {
        return CubeState.fromStickers(key());
    }

    /** Read-only access used by heuristics. */
    public char stickerAt(int index) {
        return stickers[index];
    }

    /** The letter a sticker should show on a solved cube. */
    public static char solvedStickerAt(int index) {
        return SOLVED_STICKERS[index];
    }

    /**
     * Creates a random list of moves. Two moves in a row never turn the same
     * face, because that would just merge into one move (R then R2 = R').
     */
    public static List<Move> randomScramble(int length, Random random) {
        if (length < 0) {
            throw new IllegalArgumentException("Scramble length must not be negative");
        }
        List<Move> scramble = new ArrayList<>();
        Move previous = null;

        while (scramble.size() < length) {
            Move candidate = Move.ALL.get(random.nextInt(Move.ALL.size()));
            if (candidate.sameFace(previous)) {
                continue;
            }
            scramble.add(candidate);
            previous = candidate;
        }
        return scramble;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Cube)) {
            return false;
        }
        return Arrays.equals(stickers, ((Cube) other).stickers);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(stickers);
    }

    @Override
    public String toString() {
        return key();
    }

    private static char[] buildSolvedStickers() {
        char[] solved = new char[CubeGeometry.STICKER_COUNT];
        for (int i = 0; i < solved.length; i++) {
            solved[i] = FACE_LETTERS.charAt(i / CubeGeometry.STICKERS_PER_FACE);
        }
        return solved;
    }
}
