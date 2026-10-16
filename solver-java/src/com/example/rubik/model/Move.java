package com.example.rubik.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * One move of the cube, for example R, U' or F2.
 *
 * turns = 1 means a clockwise quarter turn (R)
 * turns = 2 means a half turn (R2)
 * turns = 3 means a counter-clockwise quarter turn (R')
 *
 * A Move is immutable: once created it never changes.
 */
public final class Move {

    /** All 18 moves, in a fixed order. */
    public static final List<Move> ALL = buildAllMoves();

    private final Face face;
    private final int turns;

    private Move(Face face, int turns) {
        this.face = face;
        this.turns = turns;
    }

    public static Move of(Face face, int turns) {
        if (turns < 1 || turns > 3) {
            throw new IllegalArgumentException("turns must be 1, 2 or 3 but was " + turns);
        }
        return new Move(face, turns);
    }

    /** Parses text such as "R", "U'" or "F2". */
    public static Move parse(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Move must not be null");
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty() || trimmed.length() > 2) {
            throw new IllegalArgumentException("Invalid move: '" + text + "'");
        }

        Face face = parseFace(trimmed, text);
        int turns = parseTurns(trimmed, text);
        return new Move(face, turns);
    }

    private static Face parseFace(String trimmed, String original) {
        try {
            return Face.fromLetter(trimmed.charAt(0));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid move: '" + original + "'");
        }
    }

    private static int parseTurns(String trimmed, String original) {
        if (trimmed.length() == 1) {
            return 1;
        }
        char suffix = trimmed.charAt(1);
        if (suffix == '\'') {
            return 3;
        }
        if (suffix == '2') {
            return 2;
        }
        throw new IllegalArgumentException("Invalid move: '" + original + "'");
    }

    public Face getFace() {
        return face;
    }

    public int getTurns() {
        return turns;
    }

    /** The move that undoes this move: R becomes R', R' becomes R, R2 stays R2. */
    public Move inverse() {
        return new Move(face, 4 - turns);
    }

    /** True when both moves turn the same face (for example R and R2). */
    public boolean sameFace(Move other) {
        return other != null && face == other.face;
    }

    /** Returns the sequence that undoes the given sequence (reversed, each move inverted). */
    public static List<Move> invertSequence(List<Move> moves) {
        List<Move> inverted = new ArrayList<>();
        for (Move move : moves) {
            inverted.add(move.inverse());
        }
        Collections.reverse(inverted);
        return inverted;
    }

    /** Parses a list of move strings. */
    public static List<Move> parseAll(List<String> texts) {
        List<Move> moves = new ArrayList<>();
        for (String text : texts) {
            moves.add(parse(text));
        }
        return moves;
    }

    /** Converts moves back to strings such as ["R", "U'"]. */
    public static List<String> toStrings(List<Move> moves) {
        List<String> texts = new ArrayList<>();
        for (Move move : moves) {
            texts.add(move.toString());
        }
        return texts;
    }

    private static List<Move> buildAllMoves() {
        List<Move> moves = new ArrayList<>();
        for (Face face : Face.values()) {
            for (int turns = 1; turns <= 3; turns++) {
                moves.add(new Move(face, turns));
            }
        }
        return Collections.unmodifiableList(moves);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Move)) {
            return false;
        }
        Move that = (Move) other;
        return face == that.face && turns == that.turns;
    }

    @Override
    public int hashCode() {
        return Objects.hash(face, turns);
    }

    @Override
    public String toString() {
        String suffix = switch (turns) {
            case 2 -> "2";
            case 3 -> "'";
            default -> "";
        };
        return face.getLetter() + suffix;
    }
}
