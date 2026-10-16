package com.example.rubik.solver;

import com.example.rubik.model.Cube;
import com.example.rubik.model.Face;
import com.example.rubik.model.Move;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Two-Phase solver (the idea behind Kociemba's algorithm). It can solve ANY
 * legal scramble, usually in about 20-30 moves. The answer is short but not
 * guaranteed to be the shortest.
 *
 * The big idea: split the hard problem into two easier ones.
 *
 *   Phase 1: reach a "half-solved" group G1 using all 18 moves.
 *            In G1 every corner is untwisted, every edge is unflipped and the
 *            4 middle-layer edges are somewhere in the middle layer.
 *   Phase 2: finish the cube using only the 10 moves that never leave G1:
 *            U, U2, U', D, D2, D', R2, L2, F2, B2.
 *
 * Each phase is an IDA* search. Instead of looking at the whole cube, each
 * phase looks at small numbers called COORDINATES (for example "twist" is one
 * number from 0 to 2186 that describes all corner twists). For every
 * coordinate we pre-compute:
 *   - a MOVE TABLE:   coordinate after a move  (just a table lookup)
 *   - a PRUNE TABLE:  the exact number of moves needed to fix that coordinate
 *                     alone. It is the heuristic h(n) of IDA*, and it never
 *                     overestimates (admissible).
 * The tables are built once, the first time this solver is used.
 */
public class TwoPhaseSolver implements Solver {

    // Order used for moves here: U R F D L B. Move number = faceNumber * 3 + (turns - 1).
    private static final Face[] FACE_ORDER = {Face.U, Face.R, Face.F, Face.D, Face.L, Face.B};
    private static final int MOVE_COUNT = 18;

    // Sizes of the coordinates.
    private static final int TWIST_SIZE = 2187;      // 3^7
    private static final int FLIP_SIZE = 2048;       // 2^11
    private static final int SLICE_SIZE = 495;       // C(12,4)
    private static final int PERM8_SIZE = 40320;     // 8!
    private static final int SLICE_PERM_SIZE = 24;   // 4!

    private static final int MAX_PHASE1_DEPTH = 12;
    private static final int MAX_PHASE2_DEPTH = 18;
    private static final long TIME_LIMIT_MS = 15_000;

    private static Tables tables;

    @Override
    public String getName() {
        return "Two-Phase";
    }

    @Override
    public SolverResult solve(Cube start, int maxDepth, long maxStates) {
        if (start.isSolved()) {
            return SolverResult.solved(List.of(), 0);
        }

        CubieCube cubies = CubieCube.fromCube(start);
        String problem = cubies.problem();
        if (problem != null) {
            throw new IllegalArgumentException("This cube cannot be solved. " + problem);
        }

        Search search = new Search(getTables(), cubies, maxDepth, maxStates);
        boolean found = search.run();

        if (found) {
            return SolverResult.solved(search.solution, search.nodes);
        }
        if (search.limitHit) {
            return SolverResult.limitReached(search.nodes);
        }
        return SolverResult.notFound(search.nodes);
    }

    private static synchronized Tables getTables() {
        if (tables == null) {
            tables = new Tables();
        }
        return tables;
    }

    // =====================================================================
    //  The search
    // =====================================================================

    private static final class Search {
        private final Tables t;
        private final CubieCube start;
        private final int maxTotalDepth;
        private final long nodeLimit;
        private final long deadline;

        private final int[] path = new int[64];
        private int pathLength = 0;
        private long nodes = 0;
        private boolean limitHit = false;
        private List<Move> solution = null;

        private Search(Tables t, CubieCube start, int maxTotalDepth, long nodeLimit) {
            this.t = t;
            this.start = start;
            this.maxTotalDepth = Math.min(maxTotalDepth, path.length - 1);
            this.nodeLimit = nodeLimit;
            this.deadline = System.currentTimeMillis() + TIME_LIMIT_MS;
        }

        /** Phase 1, with IDA*: try depth 0, 1, 2, ... until phase 1 and then phase 2 succeed. */
        private boolean run() {
            int twist = twistOf(start.co);
            int flip = flipOf(start.eo);
            int slice = t.maskToIndex[sliceMaskOf(start.ep)];

            for (int depth = 0; depth <= MAX_PHASE1_DEPTH && depth <= maxTotalDepth; depth++) {
                if (phase1(twist, flip, slice, depth, -1)) {
                    return true;
                }
                if (limitHit) {
                    return false;
                }
            }
            return false;
        }

        private boolean phase1(int twist, int flip, int slice, int movesLeft, int lastFace) {
            int estimate = Math.max(
                    t.twistSlicePrune[twist * SLICE_SIZE + slice],
                    t.flipSlicePrune[flip * SLICE_SIZE + slice]);
            if (estimate > movesLeft) {
                return false;
            }
            if (movesLeft == 0) {
                // estimate == 0 means twist, flip and slice are all solved: we are in G1.
                return startPhase2();
            }

            for (int move = 0; move < MOVE_COUNT; move++) {
                if (skipMove(move / 3, lastFace)) {
                    continue;
                }
                if (outOfBudget()) {
                    return false;
                }
                path[pathLength++] = move;
                boolean found = phase1(
                        t.twistMove[twist][move],
                        t.flipMove[flip][move],
                        t.sliceMove[slice][move],
                        movesLeft - 1,
                        move / 3);
                if (found) {
                    return true;
                }
                pathLength--;
            }
            return false;
        }

        /** Phase 1 finished: work out the phase-2 coordinates and search again. */
        private boolean startPhase2() {
            int remaining = maxTotalDepth - pathLength;
            if (remaining < 0) {
                return false;
            }

            CubieCube cube = start;
            for (int i = 0; i < pathLength; i++) {
                cube = cube.multiply(t.moveCubes[path[i]]);
            }

            int cornerPerm = permutationRank(cube.cp, 0, 8);
            int edgePerm = permutationRank(cube.ep, 0, 8);
            int slicePerm = permutationRank(cube.ep, 8, 4);
            int phase1Length = pathLength;
            int lastFace = pathLength == 0 ? -1 : path[pathLength - 1] / 3;

            int limit = Math.min(remaining, MAX_PHASE2_DEPTH);
            for (int depth = 0; depth <= limit; depth++) {
                if (phase2(cornerPerm, edgePerm, slicePerm, depth, lastFace)) {
                    solution = toMoves(path, pathLength);
                    return true;
                }
                pathLength = phase1Length;
                if (limitHit) {
                    return false;
                }
            }
            return false;
        }

        private boolean phase2(int cornerPerm, int edgePerm, int slicePerm, int movesLeft, int lastFace) {
            int estimate = Math.max(
                    t.cornerSlicePrune[cornerPerm * SLICE_PERM_SIZE + slicePerm],
                    t.edgeSlicePrune[edgePerm * SLICE_PERM_SIZE + slicePerm]);
            if (estimate > movesLeft) {
                return false;
            }
            if (movesLeft == 0) {
                return true;
            }

            for (int i = 0; i < t.phase2Moves.length; i++) {
                int move = t.phase2Moves[i];
                if (skipMove(move / 3, lastFace)) {
                    continue;
                }
                if (outOfBudget()) {
                    return false;
                }
                path[pathLength++] = move;
                boolean found = phase2(
                        t.cornerMove[cornerPerm][i],
                        t.edgeMove[edgePerm][i],
                        t.slicePermMove[slicePerm][i],
                        movesLeft - 1,
                        move / 3);
                if (found) {
                    return true;
                }
                pathLength--;
            }
            return false;
        }

        /** Never turn the same face twice in a row, and only one order for opposite faces. */
        private boolean skipMove(int face, int lastFace) {
            if (face == lastFace) {
                return true;
            }
            boolean oppositeFaces = lastFace >= 0 && face % 3 == lastFace % 3;
            return oppositeFaces && face < lastFace;
        }

        private boolean outOfBudget() {
            nodes++;
            if (nodes >= nodeLimit || ((nodes & 0xFFFF) == 0 && System.currentTimeMillis() > deadline)) {
                limitHit = true;
            }
            return limitHit;
        }
    }

    /** Turns move numbers into Move objects and merges neighbours such as R + R2 = R'. */
    private static List<Move> toMoves(int[] path, int length) {
        List<Move> result = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            Face face = FACE_ORDER[path[i] / 3];
            int turns = path[i] % 3 + 1;

            if (!result.isEmpty() && result.get(result.size() - 1).getFace() == face) {
                Move previous = result.remove(result.size() - 1);
                turns = (previous.getTurns() + turns) % 4;
            }
            if (turns != 0) {
                result.add(Move.of(face, turns));
            }
        }
        return result;
    }

    // =====================================================================
    //  Coordinates: turn the pieces into small numbers
    // =====================================================================

    /** Corner twists -> 0..2186 (the 8th twist is implied by the other 7). */
    private static int twistOf(int[] co) {
        int value = 0;
        for (int i = 6; i >= 0; i--) {
            value = value * 3 + co[i];
        }
        return value;
    }

    private static int[] twistToCorners(int value) {
        int[] co = new int[8];
        int sum = 0;
        for (int i = 0; i < 7; i++) {
            co[i] = value % 3;
            value /= 3;
            sum += co[i];
        }
        co[7] = (3 - sum % 3) % 3;
        return co;
    }

    /** Edge flips -> 0..2047 (the 12th flip is implied). */
    private static int flipOf(int[] eo) {
        int value = 0;
        for (int i = 10; i >= 0; i--) {
            value = value * 2 + eo[i];
        }
        return value;
    }

    private static int[] flipToEdges(int value) {
        int[] eo = new int[12];
        int sum = 0;
        for (int i = 0; i < 11; i++) {
            eo[i] = value % 2;
            value /= 2;
            sum += eo[i];
        }
        eo[11] = sum % 2;
        return eo;
    }

    /** Which 4 of the 12 edge slots hold a middle-layer edge (pieces 8..11), as a bit mask. */
    private static int sliceMaskOf(int[] ep) {
        int mask = 0;
        for (int slot = 0; slot < 12; slot++) {
            if (ep[slot] >= 8) {
                mask |= 1 << slot;
            }
        }
        return mask;
    }

    /** Position of a permutation in the sorted list of all permutations (identity = 0). */
    private static int permutationRank(int[] values, int offset, int size) {
        int rank = 0;
        for (int i = 0; i < size; i++) {
            int smallerAfter = 0;
            for (int j = i + 1; j < size; j++) {
                if (values[offset + j] < values[offset + i]) {
                    smallerAfter++;
                }
            }
            rank = rank * (size - i) + smallerAfter;
        }
        return rank;
    }

    /** The reverse of permutationRank. */
    private static int[] permutationFromRank(int rank, int size) {
        int[] digits = new int[size];
        for (int i = size - 1; i >= 0; i--) {
            digits[i] = rank % (size - i);
            rank /= (size - i);
        }
        List<Integer> available = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            available.add(i);
        }
        int[] result = new int[size];
        for (int i = 0; i < size; i++) {
            result[i] = available.remove(digits[i]);
        }
        return result;
    }

    // =====================================================================
    //  Tables (built once)
    // =====================================================================

    private static final class Tables {
        final CubieCube[] moveCubes = new CubieCube[MOVE_COUNT];
        final int[] phase2Moves;

        final int[] maskToIndex = new int[4096];
        final int[] indexToMask = new int[SLICE_SIZE];

        final int[][] twistMove = new int[TWIST_SIZE][MOVE_COUNT];
        final int[][] flipMove = new int[FLIP_SIZE][MOVE_COUNT];
        final int[][] sliceMove = new int[SLICE_SIZE][MOVE_COUNT];

        final int[][] cornerMove;
        final int[][] edgeMove;
        final int[][] slicePermMove;

        final byte[] twistSlicePrune;
        final byte[] flipSlicePrune;
        final byte[] cornerSlicePrune;
        final byte[] edgeSlicePrune;

        Tables() {
            buildMoveCubes();
            phase2Moves = findPhase2Moves();
            buildSliceNumbering();

            buildPhase1MoveTables();
            cornerMove = new int[PERM8_SIZE][phase2Moves.length];
            edgeMove = new int[PERM8_SIZE][phase2Moves.length];
            slicePermMove = new int[SLICE_PERM_SIZE][phase2Moves.length];
            buildPhase2MoveTables();

            twistSlicePrune = buildPrune(TWIST_SIZE, SLICE_SIZE, twistMove, sliceMove, MOVE_COUNT);
            flipSlicePrune = buildPrune(FLIP_SIZE, SLICE_SIZE, flipMove, sliceMove, MOVE_COUNT);
            cornerSlicePrune = buildPrune(PERM8_SIZE, SLICE_PERM_SIZE, cornerMove, slicePermMove, phase2Moves.length);
            edgeSlicePrune = buildPrune(PERM8_SIZE, SLICE_PERM_SIZE, edgeMove, slicePermMove, phase2Moves.length);
        }

        private void buildMoveCubes() {
            for (int face = 0; face < 6; face++) {
                for (int turns = 1; turns <= 3; turns++) {
                    moveCubes[face * 3 + turns - 1] = CubieCube.forMove(Move.of(FACE_ORDER[face], turns));
                }
            }
        }

        /** Phase 2 may only use U, U2, U', D, D2, D' and the half turns R2, L2, F2, B2. */
        private int[] findPhase2Moves() {
            List<Integer> moves = new ArrayList<>();
            for (int face = 0; face < 6; face++) {
                boolean upOrDown = FACE_ORDER[face] == Face.U || FACE_ORDER[face] == Face.D;
                for (int turns = 1; turns <= 3; turns++) {
                    if (upOrDown || turns == 2) {
                        moves.add(face * 3 + turns - 1);
                    }
                }
            }
            return moves.stream().mapToInt(Integer::intValue).toArray();
        }

        /** Gives each of the 495 "slice slot sets" a number. The solved set (slots 8-11) is 0. */
        private void buildSliceNumbering() {
            Arrays.fill(maskToIndex, -1);
            int index = 0;
            for (int mask = 4095; mask >= 0; mask--) {
                if (Integer.bitCount(mask) == 4) {
                    maskToIndex[mask] = index;
                    indexToMask[index] = mask;
                    index++;
                }
            }
        }

        private void buildPhase1MoveTables() {
            for (int move = 0; move < MOVE_COUNT; move++) {
                CubieCube turn = moveCubes[move];

                for (int twist = 0; twist < TWIST_SIZE; twist++) {
                    CubieCube cube = CubieCube.solved();
                    System.arraycopy(twistToCorners(twist), 0, cube.co, 0, 8);
                    twistMove[twist][move] = twistOf(cube.multiply(turn).co);
                }
                for (int flip = 0; flip < FLIP_SIZE; flip++) {
                    CubieCube cube = CubieCube.solved();
                    System.arraycopy(flipToEdges(flip), 0, cube.eo, 0, 12);
                    flipMove[flip][move] = flipOf(cube.multiply(turn).eo);
                }
                for (int slice = 0; slice < SLICE_SIZE; slice++) {
                    CubieCube cube = cubeWithSliceEdgesAt(indexToMask[slice]);
                    sliceMove[slice][move] = maskToIndex[sliceMaskOf(cube.multiply(turn).ep)];
                }
            }
        }

        /** A cube whose middle-layer edges (8..11) sit in the slots of the mask. */
        private CubieCube cubeWithSliceEdgesAt(int mask) {
            CubieCube cube = CubieCube.solved();
            int nextSlicePiece = 8;
            int nextOtherPiece = 0;
            for (int slot = 0; slot < 12; slot++) {
                if ((mask & (1 << slot)) != 0) {
                    cube.ep[slot] = nextSlicePiece++;
                } else {
                    cube.ep[slot] = nextOtherPiece++;
                }
            }
            return cube;
        }

        private void buildPhase2MoveTables() {
            for (int i = 0; i < phase2Moves.length; i++) {
                CubieCube turn = moveCubes[phase2Moves[i]];

                for (int rank = 0; rank < PERM8_SIZE; rank++) {
                    int[] permutation = permutationFromRank(rank, 8);

                    CubieCube corners = CubieCube.solved();
                    System.arraycopy(permutation, 0, corners.cp, 0, 8);
                    cornerMove[rank][i] = permutationRank(corners.multiply(turn).cp, 0, 8);

                    CubieCube edges = CubieCube.solved();
                    System.arraycopy(permutation, 0, edges.ep, 0, 8);
                    edgeMove[rank][i] = permutationRank(edges.multiply(turn).ep, 0, 8);
                }

                for (int rank = 0; rank < SLICE_PERM_SIZE; rank++) {
                    int[] permutation = permutationFromRank(rank, 4);
                    CubieCube cube = CubieCube.solved();
                    for (int k = 0; k < 4; k++) {
                        cube.ep[8 + k] = 8 + permutation[k];
                    }
                    slicePermMove[rank][i] = permutationRank(cube.multiply(turn).ep, 8, 4);
                }
            }
        }

        /**
         * Breadth-first search over a pair of coordinates, starting from the solved
         * pair (0, 0). table[a * sizeB + b] = fewest moves needed to solve both.
         */
        private byte[] buildPrune(int sizeA, int sizeB, int[][] moveA, int[][] moveB, int moveCount) {
            byte[] table = new byte[sizeA * sizeB];
            Arrays.fill(table, (byte) -1);
            table[0] = 0;

            for (int depth = 0; ; depth++) {
                boolean foundAny = false;
                for (int index = 0; index < table.length; index++) {
                    if (table[index] != depth) {
                        continue;
                    }
                    foundAny = true;
                    int a = index / sizeB;
                    int b = index % sizeB;
                    for (int move = 0; move < moveCount; move++) {
                        int next = moveA[a][move] * sizeB + moveB[b][move];
                        if (table[next] == -1) {
                            table[next] = (byte) (depth + 1);
                        }
                    }
                }
                if (!foundAny) {
                    return table;
                }
            }
        }
    }
}
