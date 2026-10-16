package com.example.rubik;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * The program the Node.js backend starts. It stays running and answers
 * one command per line, so the slow table building happens only once.
 *
 * Input line:   <id> <COMMAND> <arguments...>
 * Output line:  <id> <json>
 */
public class SolverServer {

    public static void main(String[] args) throws Exception {
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        CommandHandler handler = new CommandHandler();

        out.println("READY");

        String line;
        while ((line = in.readLine()) != null) {
            if (line.isBlank()) {
                continue;
            }
            String[] words = line.trim().split(" ");
            String id = words[0];
            String[] command = new String[words.length - 1];
            System.arraycopy(words, 1, command, 0, command.length);
            out.println(id + " " + answer(handler, command));
        }
    }

    /** Runs the command and turns any error into a JSON error message. */
    private static String answer(CommandHandler handler, String[] command) {
        try {
            return handler.handle(command);
        } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
            // Bad input from the user (bad move, bad cube, bad number).
            return Json.object().put("success", false).put("kind", "invalid")
                    .put("error", String.valueOf(e.getMessage())).build();
        } catch (Throwable e) {
            // A bug or an out-of-memory problem on our side.
            return Json.object().put("success", false).put("kind", "internal")
                    .put("error", "Solver error: " + e).build();
        }
    }
}
