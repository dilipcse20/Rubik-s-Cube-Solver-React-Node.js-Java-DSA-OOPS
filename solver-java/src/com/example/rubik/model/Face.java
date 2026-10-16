package com.example.rubik.model;

/**
 * The six faces of the cube.
 *
 * Each face knows its letter and the direction it points to in 3D space
 * (its "normal"). The 3D coordinates are what make our rotations correct:
 * x goes Left (-1) to Right (+1), y goes Down (-1) to Up (+1),
 * and z goes Back (-1) to Front (+1).
 */
public enum Face {

    U('U', 0, 1, 0),
    D('D', 0, -1, 0),
    F('F', 0, 0, 1),
    B('B', 0, 0, -1),
    L('L', -1, 0, 0),
    R('R', 1, 0, 0);

    private final char letter;
    private final int[] normal;

    Face(char letter, int x, int y, int z) {
        this.letter = letter;
        this.normal = new int[] {x, y, z};
    }

    public char getLetter() {
        return letter;
    }

    /** Returns a copy of the (x, y, z) direction this face points to. */
    public int[] getNormal() {
        return normal.clone();
    }

    /** The axis (0 = x, 1 = y, 2 = z) this face is perpendicular to. */
    public int getAxis() {
        for (int axis = 0; axis < 3; axis++) {
            if (normal[axis] != 0) {
                return axis;
            }
        }
        throw new IllegalStateException("Face has no axis: " + this);
    }

    /** +1 if the face is on the positive side of its axis, -1 otherwise. */
    public int getSign() {
        return normal[getAxis()];
    }

    /**
     * Converts a sticker position on this face (row 0-2, column 0-2, as seen
     * when the face is drawn in the standard cube net) into 3D coordinates.
     */
    public int[] stickerPosition(int row, int col) {
        return switch (this) {
            case U -> new int[] {col - 1, 1, row - 1};
            case D -> new int[] {col - 1, -1, 1 - row};
            case F -> new int[] {col - 1, 1 - row, 1};
            case B -> new int[] {1 - col, 1 - row, -1};
            case L -> new int[] {-1, 1 - row, col - 1};
            case R -> new int[] {1, 1 - row, 1 - col};
        };
    }

    public static Face fromLetter(char letter) {
        for (Face face : values()) {
            if (face.letter == letter) {
                return face;
            }
        }
        throw new IllegalArgumentException("Unknown face letter: " + letter);
    }
}
