package com.example.rubik.solver;

import com.example.rubik.model.Cube;
import com.example.rubik.model.CubeGeometry;
import com.example.rubik.model.Face;
import com.example.rubik.model.Move;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A second way to look at the cube: as 8 corner pieces and 12 edge pieces.
 *
 * The sticker model (Cube) is great for simple searches. The Two-Phase solver
 * needs to talk about PIECES, so this class converts a Cube into:
 *   cp[i] = which corner piece sits in corner slot i
 *   co[i] = how much that corner is twisted (0, 1 or 2)
 *   ep[i] = which edge piece sits in edge slot i
 *   eo[i] = whether that edge is flipped (0 or 1)
 *
 * Moves are NOT typed in by hand. We turn a solved sticker cube, read the
 * pieces, and get each basic move as a CubieCube. Combining moves is then
 * done with multiply().
 */
public final class CubieCube {

    // Corner slots (x, y, z) and edge slots (x, y, z) around the cube.
    private static final int[][] CORNER_SLOTS = {
        {1, 1, 1}, {-1, 1, 1}, {-1, 1, -1}, {1, 1, -1},
        {1, -1, 1}, {-1, -1, 1}, {-1, -1, -1}, {1, -1, -1}
    };
    private static final int[][] EDGE_SLOTS = {
        {1, 1, 0}, {0, 1, 1}, {-1, 1, 0}, {0, 1, -1},
        {1, -1, 0}, {0, -1, 1}, {-1, -1, 0}, {0, -1, -1},
        {1, 0, 1}, {-1, 0, 1}, {-1, 0, -1}, {1, 0, -1}
    };

    // Sticker numbers of every slot. The first sticker is the "reference" sticker.
    // Corners list their stickers clockwise, starting with the U or D sticker.
    private static final int[][] CORNER_STICKERS = new int[8][3];
    private static final int[][] EDGE_STICKERS = new int[12][2];

    // Pieces are recognised by their (sorted) colours, for example "FRU".
    private static final Map<String, Integer> CORNER_BY_COLOURS = new HashMap<>();
    private static final Map<String, Integer> EDGE_BY_COLOURS = new HashMap<>();

    // The 6 basic clockwise moves, in the order of Face.values().
    private static final CubieCube[] BASIC_MOVES = new CubieCube[6];

    public final int[] cp = new int[8];
    public final int[] co = new int[8];
    public final int[] ep = new int[12];
    public final int[] eo = new int[12];

    static {
        buildStickerTables();
        buildPieceNames();
        buildBasicMoves();
    }

    private static void buildStickerTables() {
        for (int i = 0; i < 8; i++) {
            int[] slot = CORNER_SLOTS[i];
            Face upDown = slot[1] > 0 ? Face.U : Face.D;
            Face side = slot[0] > 0 ? Face.R : Face.L;
            Face frontBack = slot[2] > 0 ? Face.F : Face.B;

            // Clockwise order (seen from outside) means the "triple product" is -1.
            boolean sideFirst = tripleProduct(upDown, side, frontBack) == -1;
            Face second = sideFirst ? side : frontBack;
            Face third = sideFirst ? frontBack : side;

            CORNER_STICKERS[i][0] = stickerAt(upDown, slot);
            CORNER_STICKERS[i][1] = stickerAt(second, slot);
            CORNER_STICKERS[i][2] = stickerAt(third, slot);
        }

        for (int i = 0; i < 12; i++) {
            int[] slot = EDGE_SLOTS[i];
            Face first;
            Face second;
            if (slot[1] != 0) {
                first = slot[1] > 0 ? Face.U : Face.D;
                second = slot[0] != 0 ? (slot[0] > 0 ? Face.R : Face.L) : (slot[2] > 0 ? Face.F : Face.B);
            } else {
                first = slot[2] > 0 ? Face.F : Face.B;
                second = slot[0] > 0 ? Face.R : Face.L;
            }
            EDGE_STICKERS[i][0] = stickerAt(first, slot);
            EDGE_STICKERS[i][1] = stickerAt(second, slot);
        }
    }

    private static int tripleProduct(Face a, Face b, Face c) {
        int[] x = a.getNormal();
        int[] y = b.getNormal();
        int[] z = c.getNormal();
        int crossX = x[1] * y[2] - x[2] * y[1];
        int crossY = x[2] * y[0] - x[0] * y[2];
        int crossZ = x[0] * y[1] - x[1] * y[0];
        return crossX * z[0] + crossY * z[1] + crossZ * z[2];
    }

    /** Finds the sticker of a face that sits at a given 3D slot. */
    private static int stickerAt(Face face, int[] slot) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (Arrays.equals(face.stickerPosition(row, col), slot)) {
                    return CubeGeometry.stickerIndex(face, row, col);
                }
            }
        }
        throw new IllegalStateException("No sticker for face " + face);
    }

    private static void buildPieceNames() {
        for (int i = 0; i < 8; i++) {
            CORNER_BY_COLOURS.put(solvedColours(CORNER_STICKERS[i]), i);
        }
        for (int i = 0; i < 12; i++) {
            EDGE_BY_COLOURS.put(solvedColours(EDGE_STICKERS[i]), i);
        }
    }

    private static String solvedColours(int[] stickers) {
        char[] colours = new char[stickers.length];
        for (int i = 0; i < stickers.length; i++) {
            colours[i] = Cube.solvedStickerAt(stickers[i]);
        }
        Arrays.sort(colours);
        return new String(colours);
    }

    private static void buildBasicMoves() {
        for (Face face : Face.values()) {
            Cube turned = Cube.solved().applied(Move.of(face, 1));
            BASIC_MOVES[face.ordinal()] = fromCube(turned);
        }
    }

    // ------------------------------------------------------------------

    public static CubieCube solved() {
        CubieCube cube = new CubieCube();
        for (int i = 0; i < 8; i++) {
            cube.cp[i] = i;
        }
        for (int i = 0; i < 12; i++) {
            cube.ep[i] = i;
        }
        return cube;
    }

    /** The cubie version of one move (R, U2, F' ...). */
    public static CubieCube forMove(Move move) {
        CubieCube result = solved();
        for (int i = 0; i < move.getTurns(); i++) {
            result = result.multiply(BASIC_MOVES[move.getFace().ordinal()]);
        }
        return result;
    }

    /** Reads the pieces out of a sticker cube. Throws if the stickers make no real piece. */
    public static CubieCube fromCube(Cube cube) {
        CubieCube result = new CubieCube();

        for (int slot = 0; slot < 8; slot++) {
            char[] colours = new char[3];
            int twist = -1;
            for (int k = 0; k < 3; k++) {
                colours[k] = cube.stickerAt(CORNER_STICKERS[slot][k]);
                if (colours[k] == 'U' || colours[k] == 'D') {
                    twist = k;
                }
            }
            Arrays.sort(colours);
            Integer piece = CORNER_BY_COLOURS.get(new String(colours));
            if (piece == null || twist < 0) {
                throw new IllegalArgumentException("Corner " + slot + " is not a real corner piece");
            }
            result.cp[slot] = piece;
            result.co[slot] = twist;
        }

        for (int slot = 0; slot < 12; slot++) {
            char first = cube.stickerAt(EDGE_STICKERS[slot][0]);
            char second = cube.stickerAt(EDGE_STICKERS[slot][1]);
            char[] colours = {first, second};
            Arrays.sort(colours);
            Integer piece = EDGE_BY_COLOURS.get(new String(colours));
            if (piece == null) {
                throw new IllegalArgumentException("Edge " + slot + " is not a real edge piece");
            }
            result.ep[slot] = piece;
            char referenceColour = Cube.solvedStickerAt(EDGE_STICKERS[piece][0]);
            result.eo[slot] = first == referenceColour ? 0 : 1;
        }

        result.checkNoDuplicates();
        return result;
    }

    private void checkNoDuplicates() {
        boolean[] cornerSeen = new boolean[8];
        boolean[] edgeSeen = new boolean[12];
        for (int piece : cp) {
            if (cornerSeen[piece]) {
                throw new IllegalArgumentException("The same corner piece appears twice");
            }
            cornerSeen[piece] = true;
        }
        for (int piece : ep) {
            if (edgeSeen[piece]) {
                throw new IllegalArgumentException("The same edge piece appears twice");
            }
            edgeSeen[piece] = true;
        }
    }

    /** "this, then move b". The same formula that group theory uses for cube moves. */
    public CubieCube multiply(CubieCube b) {
        CubieCube result = new CubieCube();
        for (int i = 0; i < 8; i++) {
            result.cp[i] = cp[b.cp[i]];
            result.co[i] = (co[b.cp[i]] + b.co[i]) % 3;
        }
        for (int i = 0; i < 12; i++) {
            result.ep[i] = ep[b.ep[i]];
            result.eo[i] = (eo[b.ep[i]] + b.eo[i]) % 2;
        }
        return result;
    }

    /**
     * Returns null if this cube can be reached by turning a real cube,
     * otherwise a message explaining the problem (twisted corner, flipped edge,
     * or two pieces swapped).
     */
    public String problem() {
        if (sum(co) % 3 != 0) {
            return "One corner is twisted in place. Re-check the colours of the corners.";
        }
        if (sum(eo) % 2 != 0) {
            return "One edge is flipped in place. Re-check the colours of the edges.";
        }
        if (parity(cp) != parity(ep)) {
            return "Two pieces are swapped. Re-check the colours of the corners and edges.";
        }
        return null;
    }

    private static int sum(int[] values) {
        int total = 0;
        for (int value : values) {
            total += value;
        }
        return total;
    }

    /** 0 for an even permutation, 1 for an odd one (counts swapped pairs). */
    private static int parity(int[] permutation) {
        int inversions = 0;
        for (int i = 0; i < permutation.length; i++) {
            for (int j = i + 1; j < permutation.length; j++) {
                if (permutation[i] > permutation[j]) {
                    inversions++;
                }
            }
        }
        return inversions % 2;
    }

    public boolean sameAs(CubieCube other) {
        return Arrays.equals(cp, other.cp) && Arrays.equals(co, other.co)
                && Arrays.equals(ep, other.ep) && Arrays.equals(eo, other.eo);
    }
}
