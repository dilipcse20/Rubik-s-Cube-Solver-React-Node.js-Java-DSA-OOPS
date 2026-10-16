package com.example.rubik.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A validated snapshot of a cube, used to move cube data in and out of JSON.
 *
 * In JSON a state looks like:
 * { "U": "UUUUUUUUU", "D": "DDDDDDDDD", "F": "FFFFFFFFF",
 *   "B": "BBBBBBBBB", "L": "LLLLLLLLL", "R": "RRRRRRRRR" }
 * Each string lists the 9 stickers of that face row by row (top-left first).
 *
 * What validation checks:
 *  - all six faces are present, each with exactly 9 valid letters
 *  - every letter appears exactly 9 times
 *  - every face centre has its own letter (centres never move)
 *  - every corner and edge is a real piece that appears only once
 *
 * What it does NOT check: twist and swap parity. A few unreachable states
 * can therefore pass validation. The solvers simply report "not found" for them.
 */
public final class CubeState {

    private static final String FACE_LETTERS = "UDFBLR";

    private final String stickers;

    private CubeState(String stickers) {
        this.stickers = stickers;
    }

    public static CubeState solved() {
        return new CubeState(Cube.solved().key());
    }

    /** Builds a state from a 54-letter string (faces in the order U, D, F, B, L, R). */
    public static CubeState fromStickers(String stickers) {
        if (stickers == null || stickers.length() != CubeGeometry.STICKER_COUNT) {
            throw new IllegalArgumentException("A cube needs exactly 54 stickers");
        }
        checkLetters(stickers);
        checkColourCounts(stickers);
        checkCentres(stickers);
        checkPieces(stickers);
        return new CubeState(stickers);
    }

    /** Builds a state from the JSON map of face letter to 9 stickers. */
    public static CubeState fromFaces(Map<String, String> faces) {
        if (faces == null || faces.isEmpty()) {
            throw new IllegalArgumentException("Cube state is missing");
        }
        checkFaceKeys(faces);

        StringBuilder builder = new StringBuilder();
        for (Face face : Face.values()) {
            String stickers = faces.get(String.valueOf(face.getLetter()));
            if (stickers == null || stickers.length() != CubeGeometry.STICKERS_PER_FACE) {
                throw new IllegalArgumentException(
                        "Face " + face + " must contain exactly 9 stickers");
            }
            builder.append(stickers);
        }
        return fromStickers(builder.toString());
    }

    /** Converts back to the JSON map, faces in the order U, D, F, B, L, R. */
    public Map<String, String> toFaces() {
        Map<String, String> faces = new LinkedHashMap<>();
        for (Face face : Face.values()) {
            int start = face.ordinal() * CubeGeometry.STICKERS_PER_FACE;
            String letters = stickers.substring(start, start + CubeGeometry.STICKERS_PER_FACE);
            faces.put(String.valueOf(face.getLetter()), letters);
        }
        return faces;
    }

    public String asString() {
        return stickers;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CubeState && stickers.equals(((CubeState) other).stickers);
    }

    @Override
    public int hashCode() {
        return stickers.hashCode();
    }

    @Override
    public String toString() {
        return stickers;
    }

    private static void checkFaceKeys(Map<String, String> faces) {
        for (String key : faces.keySet()) {
            if (key == null || key.length() != 1 || FACE_LETTERS.indexOf(key.charAt(0)) < 0) {
                throw new IllegalArgumentException("Unknown face in cube state: " + key);
            }
        }
        for (Face face : Face.values()) {
            if (!faces.containsKey(String.valueOf(face.getLetter()))) {
                throw new IllegalArgumentException("Cube state is missing face " + face);
            }
        }
    }

    private static void checkLetters(String stickers) {
        for (char letter : stickers.toCharArray()) {
            if (FACE_LETTERS.indexOf(letter) < 0) {
                throw new IllegalArgumentException("Invalid sticker '" + letter
                        + "'. Allowed letters are U, D, F, B, L, R");
            }
        }
    }

    private static void checkColourCounts(String stickers) {
        for (char letter : FACE_LETTERS.toCharArray()) {
            long count = stickers.chars().filter(c -> c == letter).count();
            if (count != CubeGeometry.STICKERS_PER_FACE) {
                throw new IllegalArgumentException("Colour " + letter + " appears "
                        + count + " times but must appear exactly 9 times");
            }
        }
    }

    private static void checkCentres(String stickers) {
        for (Face face : Face.values()) {
            char centre = stickers.charAt(CubeGeometry.stickerIndex(face, 1, 1));
            if (centre != face.getLetter()) {
                throw new IllegalArgumentException("The centre of face " + face
                        + " must be " + face + " but was " + centre);
            }
        }
    }

    /** Every corner and edge must show a colour set that exists on a real cube, once. */
    private static void checkPieces(String stickers) {
        checkPieceGroup(stickers, CubeGeometry.cornerStickers(), "corner");
        checkPieceGroup(stickers, CubeGeometry.edgeStickers(), "edge");
    }

    private static void checkPieceGroup(String stickers, int[][] pieces, String name) {
        Set<String> expected = new HashSet<>();
        for (int[] piece : pieces) {
            expected.add(colourSet(Cube.solved().key(), piece));
        }

        Set<String> seen = new HashSet<>();
        for (int[] piece : pieces) {
            String colours = colourSet(stickers, piece);
            if (!expected.contains(colours) || !seen.add(colours)) {
                throw new IllegalArgumentException("Invalid " + name
                        + " piece with colours " + colours);
            }
        }
    }

    /** The colours of a piece, sorted so the order of the stickers does not matter. */
    private static String colourSet(String stickers, int[] piece) {
        char[] colours = new char[piece.length];
        for (int i = 0; i < piece.length; i++) {
            colours[i] = stickers.charAt(piece[i]);
        }
        Arrays.sort(colours);
        return new String(colours);
    }
}
