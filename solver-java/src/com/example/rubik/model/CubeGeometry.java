package com.example.rubik.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pre-computes everything that depends only on the shape of the cube.
 *
 * Sticker numbering: sticker index = faceOrdinal * 9 + row * 3 + col, with the
 * faces in the order U, D, F, B, L, R. So U owns indexes 0-8, D owns 9-17 and so on.
 *
 * How moves are computed (this is the important part):
 * every sticker has a 3D position and a normal direction. To turn a face we
 * rotate the stickers in that layer by 90 degrees around the face's axis and
 * look up which sticker slot they land in. Nothing is "faked" and no
 * permutation tables are typed by hand.
 */
public final class CubeGeometry {

    public static final int FACE_COUNT = 6;
    public static final int STICKERS_PER_FACE = 9;
    public static final int STICKER_COUNT = FACE_COUNT * STICKERS_PER_FACE;

    /** For each face and turn count (1..3): destination[i] = where sticker i ends up. */
    private static final int[][][] DESTINATIONS = new int[FACE_COUNT][4][];

    private static final int[][] POSITIONS = new int[STICKER_COUNT][];
    private static final int[][] NORMALS = new int[STICKER_COUNT][];

    /** Sticker indexes grouped per corner piece (3 each) and edge piece (2 each). */
    private static final int[][] CORNER_STICKERS;
    private static final int[][] EDGE_STICKERS;

    static {
        fillPositionsAndNormals();
        fillMoveTables();
        List<int[]> corners = new ArrayList<>();
        List<int[]> edges = new ArrayList<>();
        groupStickersIntoPieces(corners, edges);
        CORNER_STICKERS = corners.toArray(new int[0][]);
        EDGE_STICKERS = edges.toArray(new int[0][]);
    }

    private CubeGeometry() {
    }

    public static int stickerIndex(Face face, int row, int col) {
        return face.ordinal() * STICKERS_PER_FACE + row * 3 + col;
    }

    /** Where does sticker i go when the given face is turned? */
    public static int[] destination(Face face, int turns) {
        return DESTINATIONS[face.ordinal()][turns];
    }

    public static int[][] cornerStickers() {
        return CORNER_STICKERS;
    }

    public static int[][] edgeStickers() {
        return EDGE_STICKERS;
    }

    private static void fillPositionsAndNormals() {
        for (Face face : Face.values()) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    int index = stickerIndex(face, row, col);
                    POSITIONS[index] = face.stickerPosition(row, col);
                    NORMALS[index] = face.getNormal();
                }
            }
        }
    }

    private static void fillMoveTables() {
        Map<String, Integer> slotByKey = new HashMap<>();
        for (int i = 0; i < STICKER_COUNT; i++) {
            slotByKey.put(slotKey(POSITIONS[i], NORMALS[i]), i);
        }

        for (Face face : Face.values()) {
            int[] quarter = buildQuarterTurn(face, slotByKey);
            int[] half = compose(quarter, quarter);
            int[] threeQuarters = compose(half, quarter);
            DESTINATIONS[face.ordinal()][1] = quarter;
            DESTINATIONS[face.ordinal()][2] = half;
            DESTINATIONS[face.ordinal()][3] = threeQuarters;
        }
    }

    private static int[] buildQuarterTurn(Face face, Map<String, Integer> slotByKey) {
        int[] destination = new int[STICKER_COUNT];
        int axis = face.getAxis();
        int sign = face.getSign();

        for (int i = 0; i < STICKER_COUNT; i++) {
            boolean inLayer = POSITIONS[i][axis] == sign;
            if (!inLayer) {
                destination[i] = i;
                continue;
            }
            int[] newPosition = rotateClockwise(POSITIONS[i], face);
            int[] newNormal = rotateClockwise(NORMALS[i], face);
            destination[i] = slotByKey.get(slotKey(newPosition, newNormal));
        }
        return destination;
    }

    /**
     * Rotates a vector 90 degrees clockwise as seen when looking at the face
     * from outside the cube. Clockwise from outside is -90 degrees around the
     * axis when the face is on the positive side, and +90 degrees otherwise.
     */
    private static int[] rotateClockwise(int[] v, Face face) {
        int x = v[0];
        int y = v[1];
        int z = v[2];
        boolean negativeTurn = face.getSign() > 0;

        return switch (face.getAxis()) {
            case 0 -> negativeTurn ? new int[] {x, z, -y} : new int[] {x, -z, y};
            case 1 -> negativeTurn ? new int[] {-z, y, x} : new int[] {z, y, -x};
            default -> negativeTurn ? new int[] {y, -x, z} : new int[] {-y, x, z};
        };
    }

    /** Applies 'first' and then 'second'. */
    private static int[] compose(int[] first, int[] second) {
        int[] result = new int[STICKER_COUNT];
        for (int i = 0; i < STICKER_COUNT; i++) {
            result[i] = second[first[i]];
        }
        return result;
    }

    private static void groupStickersIntoPieces(List<int[]> corners, List<int[]> edges) {
        Map<String, List<Integer>> piecesByPosition = new HashMap<>();
        for (int i = 0; i < STICKER_COUNT; i++) {
            String key = POSITIONS[i][0] + "," + POSITIONS[i][1] + "," + POSITIONS[i][2];
            piecesByPosition.computeIfAbsent(key, k -> new ArrayList<>()).add(i);
        }

        for (List<Integer> stickers : piecesByPosition.values()) {
            int[] indexes = stickers.stream().mapToInt(Integer::intValue).toArray();
            if (indexes.length == 3) {
                corners.add(indexes);
            } else if (indexes.length == 2) {
                edges.add(indexes);
            }
        }
    }

    private static String slotKey(int[] position, int[] normal) {
        return position[0] + "," + position[1] + "," + position[2]
                + "/" + normal[0] + "," + normal[1] + "," + normal[2];
    }
}
