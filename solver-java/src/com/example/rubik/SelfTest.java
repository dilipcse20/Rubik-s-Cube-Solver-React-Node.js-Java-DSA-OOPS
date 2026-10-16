package com.example.rubik;

import com.example.rubik.model.Cube;
import com.example.rubik.model.CubeState;
import com.example.rubik.model.Face;
import com.example.rubik.model.Move;
import com.example.rubik.solver.BFSolver;
import com.example.rubik.solver.CubieCube;
import com.example.rubik.solver.DFSolver;
import com.example.rubik.solver.IDAStarSolver;
import com.example.rubik.solver.IDDFSSolver;
import com.example.rubik.solver.Solver;
import com.example.rubik.solver.SolverResult;
import com.example.rubik.solver.TwoPhaseSolver;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Simple tests that need no libraries. Run with:
 *   java -cp out com.example.rubik.SelfTest
 * Every check prints PASS or FAIL, and the program exits with an error if any check fails.
 */
public class SelfTest {

    private static final long LOTS = 5_000_000;
    private static int failures = 0;

    public static void main(String[] args) {
        cubeMoveTests();
        scrambleTests();
        validationTests();
        cubieTests();
        solverTests();
        twoPhaseTests();

        System.out.println(failures == 0 ? "\nALL TESTS PASSED" : "\n" + failures + " TEST(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    // ---------- helpers ----------

    private static void check(boolean condition, String name) {
        System.out.println((condition ? "PASS  " : "FAIL  ") + name);
        if (!condition) {
            failures++;
        }
    }

    private static Cube scrambled(String... moves) {
        return Cube.solved().applied(Move.parseAll(List.of(moves)));
    }

    private static boolean throwsInvalid(Runnable action) {
        try {
            action.run();
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    // ---------- cube moves ----------

    private static void cubeMoveTests() {
        check(Cube.solved().isSolved(), "solved cube is recognised");

        boolean everyMoveChanges = true;
        boolean inversesWork = true;
        Cube start = scrambled("B", "L'", "U2", "R", "F'");
        for (Move move : Move.ALL) {
            everyMoveChanges &= !Cube.solved().applied(move).isSolved();
            inversesWork &= start.applied(move).applied(move.inverse()).equals(start);
        }
        check(everyMoveChanges, "every move changes a solved cube");
        check(inversesWork, "a move followed by its inverse restores the cube");

        boolean fourTurns = true;
        boolean twoHalfTurns = true;
        for (Face face : Face.values()) {
            Move quarter = Move.of(face, 1);
            Move half = Move.of(face, 2);
            fourTurns &= start.applied(List.of(quarter, quarter, quarter, quarter)).equals(start);
            twoHalfTurns &= start.applied(List.of(half, half)).equals(start);
        }
        check(fourTurns, "four clockwise turns restore the cube");
        check(twoHalfTurns, "two half turns restore the cube");

        Map<String, String> afterR = Cube.solved().applied(Move.parse("R")).toState().toFaces();
        check(afterR.get("U").equals("UUFUUFUUF") && afterR.get("F").equals("FFDFFDFFD")
                && afterR.get("D").equals("DDBDDBDDB") && afterR.get("B").equals("UBBUBBUBB"),
                "R moves the stickers like a real cube");

        Cube cube = Cube.solved();
        boolean orderIsSix = true;
        for (int i = 1; i <= 6; i++) {
            cube = cube.applied(Move.parseAll(List.of("R", "U", "R'", "U'")));
            orderIsSix &= cube.isSolved() == (i == 6);
        }
        check(orderIsSix, "R U R' U' has order 6");

        cube = Cube.solved();
        boolean orderIs105 = true;
        for (int i = 1; i <= 105; i++) {
            cube = cube.applied(Move.parseAll(List.of("R", "U")));
            orderIs105 &= cube.isSolved() == (i == 105);
        }
        check(orderIs105, "R U has order 105");
    }

    private static void scrambleTests() {
        Random random = new Random(42);
        boolean allSolved = true;
        for (int length = 1; length <= 25; length++) {
            List<Move> scramble = Cube.randomScramble(length, random);
            Cube mixed = Cube.solved().applied(scramble);
            allSolved &= mixed.applied(Move.invertSequence(scramble)).isSolved();
        }
        check(allSolved, "scramble followed by its inverse solves the cube");

        List<Move> scramble = Cube.randomScramble(30, random);
        boolean noRepeat = scramble.size() == 30;
        for (int i = 1; i < scramble.size(); i++) {
            noRepeat &= !scramble.get(i).sameFace(scramble.get(i - 1));
        }
        check(noRepeat, "scramble has the right length and never repeats a face");

        check(throwsInvalid(() -> Move.parse("X")), "invalid move X is rejected");
        check(throwsInvalid(() -> Move.parse("R3")), "invalid move R3 is rejected");
        check(Move.parse("R'").equals(Move.parse("R").inverse()), "R' is the inverse of R");
    }

    // ---------- validation ----------

    private static void validationTests() {
        String solved = Cube.solved().key();
        check(!throwsInvalid(() -> CubeState.fromStickers(solved)), "solved state is valid");
        check(throwsInvalid(() -> CubeState.fromStickers(solved.substring(1))), "wrong length is rejected");
        check(throwsInvalid(() -> CubeState.fromStickers(solved.replace('U', 'Z'))), "unknown letter is rejected");

        char[] wrongCount = solved.toCharArray();
        wrongCount[0] = 'D';
        check(throwsInvalid(() -> CubeState.fromStickers(new String(wrongCount))), "wrong colour count is rejected");

        // Swap a U sticker and an F sticker of two different edges: counts stay at 9
        // but an edge with colours F and B would be needed, and that piece does not exist.
        char[] impossible = solved.toCharArray();
        char swap = impossible[1];
        impossible[1] = impossible[19];
        impossible[19] = swap;
        check(throwsInvalid(() -> CubeState.fromStickers(new String(impossible))), "impossible piece is rejected");
    }

    // ---------- pieces (cubies) ----------

    private static void cubieTests() {
        Random random = new Random(1);
        boolean sameAsMultiply = true;
        boolean alwaysLegal = true;
        for (int i = 0; i < 200; i++) {
            List<Move> moves = Cube.randomScramble(1 + random.nextInt(30), random);
            CubieCube fromStickers = CubieCube.fromCube(Cube.solved().applied(moves));
            CubieCube fromMultiply = CubieCube.solved();
            for (Move move : moves) {
                fromMultiply = fromMultiply.multiply(CubieCube.forMove(move));
            }
            sameAsMultiply &= fromStickers.sameAs(fromMultiply);
            alwaysLegal &= fromStickers.problem() == null;
        }
        check(sameAsMultiply, "piece model agrees with the sticker model");
        check(alwaysLegal, "every turned cube passes the legal-cube check");

        // Twist one corner in place: stickers form a real corner but the cube is impossible.
        char[] twisted = Cube.solved().key().toCharArray();
        int[] cornerStickers = {8, 20, 45};  // the three stickers of the U-R-F corner
        char first = twisted[cornerStickers[0]];
        twisted[cornerStickers[0]] = twisted[cornerStickers[1]];
        twisted[cornerStickers[1]] = twisted[cornerStickers[2]];
        twisted[cornerStickers[2]] = first;
        Cube twistedCube = Cube.fromState(CubeState.fromStickers(new String(twisted)));
        check(CubieCube.fromCube(twistedCube).problem() != null, "a twisted corner is detected");

        // Swap the UF and UR edges completely: real pieces, but impossible (odd swap).
        char[] swapped = Cube.solved().key().toCharArray();
        swapStickers(swapped, 7, 5);     // U-face stickers of the two edges
        swapStickers(swapped, 19, 46);   // their F-face and R-face stickers
        Cube swappedCube = Cube.fromState(CubeState.fromStickers(new String(swapped)));
        check(CubieCube.fromCube(swappedCube).problem() != null, "two swapped edges are detected");
    }

    private static void swapStickers(char[] stickers, int a, int b) {
        char temp = stickers[a];
        stickers[a] = stickers[b];
        stickers[b] = temp;
    }

    // ---------- simple solvers ----------

    private static void solverTests() {
        List<Solver> all = List.of(new BFSolver(), new DFSolver(), new IDDFSSolver(),
                new IDAStarSolver(), new TwoPhaseSolver());
        boolean solvedIsEmpty = true;
        for (Solver solver : all) {
            SolverResult result = solver.solve(Cube.solved(), 5, LOTS);
            solvedIsEmpty &= result.isSolved() && result.getMoves().isEmpty();
        }
        check(solvedIsEmpty, "every solver returns an empty solution for a solved cube");

        Cube shallow = scrambled("R", "U", "F'");
        boolean shallowOk = true;
        for (Solver solver : all) {
            int depthLimit = solver instanceof TwoPhaseSolver ? 30 : 4;
            SolverResult result = solver.solve(shallow, depthLimit, LOTS);
            shallowOk &= result.isSolved() && shallow.applied(result.getMoves()).isSolved();
        }
        check(shallowOk, "every solver solves a 3-move scramble");

        Cube oneMove = scrambled("R");
        SolverResult bfs = new BFSolver().solve(oneMove, 3, LOTS);
        check(bfs.isSolved() && bfs.getMoves().equals(List.of(Move.parse("R'"))), "BFS finds R' for R");

        Cube sexy = scrambled("R", "U", "R'", "U'");
        check(new IDDFSSolver().solve(sexy, 3, LOTS).getStatus() == SolverResult.Status.NOT_FOUND_WITHIN_DEPTH,
                "IDDFS says 'not found' when the depth limit is too small");
        SolverResult enough = new IDDFSSolver().solve(sexy, 4, LOTS);
        check(enough.isSolved() && enough.getMoves().size() == 4, "IDDFS finds the shortest (4 moves) solution");
        check(new DFSolver().solve(sexy, 2, LOTS).getStatus() == SolverResult.Status.NOT_FOUND_WITHIN_DEPTH,
                "DFS respects its depth limit");

        Cube harder = scrambled("R", "U", "F", "D", "L");
        check(new IDDFSSolver().solve(harder, 6, 300).getStatus() == SolverResult.Status.STATE_LIMIT_REACHED,
                "IDDFS stops at the state limit");
        check(new BFSolver().solve(harder, 6, 100).getStatus() == SolverResult.Status.STATE_LIMIT_REACHED,
                "BFS stops at the state limit");

        Random random = new Random(21);
        boolean sameLength = true;
        for (int i = 0; i < 10; i++) {
            Cube cube = Cube.solved().applied(Cube.randomScramble(4, random));
            int bfsLength = new BFSolver().solve(cube, 5, LOTS).getMoves().size();
            int iddfsLength = new IDDFSSolver().solve(cube, 5, LOTS).getMoves().size();
            int idaLength = new IDAStarSolver().solve(cube, 5, LOTS).getMoves().size();
            sameLength &= bfsLength == iddfsLength && iddfsLength == idaLength;
        }
        check(sameLength, "BFS, IDDFS and IDA* all find the same shortest length");

        IDAStarSolver ida = new IDAStarSolver();
        boolean admissible = ida.heuristic(Cube.solved()) == 0;
        for (int i = 0; i < 300; i++) {
            int length = 1 + random.nextInt(10);
            Cube cube = Cube.solved().applied(Cube.randomScramble(length, random));
            admissible &= ida.heuristic(cube) <= length;
        }
        check(admissible, "IDA* heuristic never overestimates");

        SolverResult idaResult = ida.solve(harder, 6, LOTS);
        SolverResult iddfsResult = new IDDFSSolver().solve(harder, 6, LOTS);
        check(idaResult.isSolved() && idaResult.getStatesExplored() < iddfsResult.getStatesExplored(),
                "IDA* explores fewer states than IDDFS");
    }

    // ---------- two-phase solver ----------

    private static void twoPhaseTests() {
        Random random = new Random(99);
        TwoPhaseSolver solver = new TwoPhaseSolver();
        boolean allSolved = true;
        int longest = 0;

        for (int i = 0; i < 30; i++) {
            Cube cube = Cube.solved().applied(Cube.randomScramble(40, random));
            SolverResult result = solver.solve(cube, 30, LOTS);
            allSolved &= result.isSolved() && cube.applied(result.getMoves()).isSolved();
            longest = Math.max(longest, result.getMoves().size());
        }
        check(allSolved, "Two-Phase solves 30 fully scrambled cubes");
        check(longest <= 30, "Two-Phase solutions are at most 30 moves (longest: " + longest + ")");

        Cube unsolvable = Cube.solved();
        char[] stickers = unsolvable.key().toCharArray();
        // Rotate one corner in place (an impossible cube).
        char first = stickers[8];
        stickers[8] = stickers[20];
        stickers[20] = stickers[45];
        stickers[45] = first;
        Cube twisted = Cube.fromState(CubeState.fromStickers(new String(stickers)));
        check(throwsInvalid(() -> solver.solve(twisted, 30, LOTS)), "Two-Phase rejects an impossible (twisted) cube");
    }
}
