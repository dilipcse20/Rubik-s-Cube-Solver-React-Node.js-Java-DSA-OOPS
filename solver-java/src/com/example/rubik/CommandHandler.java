package com.example.rubik;

import com.example.rubik.model.Cube;
import com.example.rubik.model.CubeState;
import com.example.rubik.model.Move;
import com.example.rubik.solver.BFSolver;
import com.example.rubik.solver.CubieCube;
import com.example.rubik.solver.DFSolver;
import com.example.rubik.solver.IDAStarSolver;
import com.example.rubik.solver.IDDFSSolver;
import com.example.rubik.solver.Solver;
import com.example.rubik.solver.SolverResult;
import com.example.rubik.solver.TwoPhaseSolver;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Runs one command and returns the answer as JSON text.
 *
 * Commands (all words separated by single spaces):
 *   PING
 *   SOLVED
 *   SCRAMBLE <moveCount>
 *   VALIDATE <54 stickers>
 *   APPLY <54 stickers> <moves separated by commas, or - for none>
 *   SOLVE <algorithm> <maxDepth> <maxStates> <54 stickers>
 *
 * The 54 stickers are 54 letters from U D F B L R: the 9 stickers of face U,
 * then D, F, B, L and R.
 */
public class CommandHandler {

    private final Map<String, Solver> solvers = new LinkedHashMap<>();
    private final Random random = new Random();

    public CommandHandler() {
        solvers.put("BFS", new BFSolver());
        solvers.put("DFS", new DFSolver());
        solvers.put("IDDFS", new IDDFSSolver());
        solvers.put("IDASTAR", new IDAStarSolver());
        solvers.put("TWOPHASE", new TwoPhaseSolver());
    }

    public String handle(String[] words) {
        String command = words[0];
        return switch (command) {
            case "PING" -> ping();
            case "SOLVED" -> solvedCube();
            case "SCRAMBLE" -> scramble(Integer.parseInt(words[1]));
            case "VALIDATE" -> validate(words[1]);
            case "APPLY" -> apply(words[1], words[2]);
            case "SOLVE" -> solve(words[1], Integer.parseInt(words[2]),
                    Long.parseLong(words[3]), words[4]);
            default -> throw new IllegalArgumentException("Unknown command: " + command);
        };
    }

    private String ping() {
        return Json.object()
                .put("success", true)
                .put("javaVersion", System.getProperty("java.version"))
                .putStrings("algorithms", List.copyOf(solvers.keySet()))
                .build();
    }

    private String solvedCube() {
        return stateJson(Cube.solved());
    }

    private String scramble(int length) {
        List<Move> moves = Cube.randomScramble(length, random);
        Cube cube = Cube.solved().applied(moves);
        return Json.object()
                .put("success", true)
                .putStrings("scramble", Move.toStrings(moves))
                .put("moveCount", moves.size())
                .putMap("state", cube.toState().toFaces())
                .build();
    }

    /** A cube is valid if the stickers make real pieces AND a real cube could look like this. */
    private String validate(String stickers) {
        Json answer = Json.object().put("success", true);
        try {
            Cube cube = Cube.fromState(CubeState.fromStickers(stickers));
            String problem = CubieCube.fromCube(cube).problem();
            if (problem == null) {
                return answer.put("valid", true).put("message", "This cube is valid and can be solved.").build();
            }
            return answer.put("valid", false).put("message", problem).build();
        } catch (IllegalArgumentException e) {
            return answer.put("valid", false).put("message", e.getMessage()).build();
        }
    }

    private String apply(String stickers, String moveText) {
        Cube cube = Cube.fromState(CubeState.fromStickers(stickers));
        if (!moveText.equals("-")) {
            cube = cube.applied(Move.parseAll(List.of(moveText.split(","))));
        }
        return stateJson(cube);
    }

    private String stateJson(Cube cube) {
        return Json.object()
                .put("success", true)
                .putMap("state", cube.toState().toFaces())
                .put("solved", cube.isSolved())
                .build();
    }

    private String solve(String algorithm, int maxDepth, long maxStates, String stickers) {
        Solver solver = solvers.get(algorithm);
        if (solver == null) {
            throw new IllegalArgumentException("Unknown algorithm '" + algorithm + "'");
        }

        CubeState inputState = CubeState.fromStickers(stickers);
        Cube start = Cube.fromState(inputState);
        String problem = CubieCube.fromCube(start).problem();
        if (problem != null) {
            throw new IllegalArgumentException("This cube cannot be solved. " + problem);
        }

        long startedAt = System.nanoTime();
        SolverResult result = solver.solve(start, maxDepth, maxStates);
        double elapsedMs = (System.nanoTime() - startedAt) / 1_000_000.0;

        // Never claim success unless the cube really ends up solved.
        Cube finalCube = start.applied(result.getMoves());
        if (result.isSolved() && !finalCube.isSolved()) {
            throw new IllegalStateException("Solver returned moves that do not solve the cube");
        }

        String message = describe(solver, result, maxDepth);
        return Json.object()
                .put("success", result.isSolved())
                .put("status", result.getStatus().name())
                .put("message", message)
                .put("error", result.isSolved() ? null : message)
                .put("algorithm", solver.getName())
                .putStrings("solution", Move.toStrings(result.getMoves()))
                .put("moveCount", result.getMoves().size())
                .put("executionTimeMs", Math.round(elapsedMs * 100.0) / 100.0)
                .put("statesExplored", result.getStatesExplored())
                .put("maxDepth", maxDepth)
                .put("maxStates", maxStates)
                .putMap("inputState", inputState.toFaces())
                .putMap("finalState", finalCube.toState().toFaces())
                .build();
    }

    private String describe(Solver solver, SolverResult result, int maxDepth) {
        int moveCount = result.getMoves().size();
        return switch (result.getStatus()) {
            case SOLVED -> moveCount == 0
                    ? "The cube is already solved."
                    : "Solved in " + moveCount + " moves using " + solver.getName() + ".";
            case NOT_FOUND_WITHIN_DEPTH ->
                    "No solution with " + maxDepth + " moves or fewer was found. Try a larger max depth.";
            case STATE_LIMIT_REACHED ->
                    "Search stopped after " + result.getStatesExplored()
                            + " states without finding a solution. Try a shallower scramble,"
                            + " a larger state limit, or the Two-Phase algorithm.";
        };
    }
}
