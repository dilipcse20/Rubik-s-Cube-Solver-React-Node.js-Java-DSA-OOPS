import { spawn } from 'node:child_process';
import readline from 'node:readline';

import { HttpError } from '../utils/HttpError.js';

// Talks to the Java solver.
//
// We start ONE Java program (SolverServer) and keep it running. We send it a
// line of text and it answers with a line of JSON. Keeping it running means
// the slow Two-Phase tables are built only once.
//
//   Node sends:    "7 SOLVE TWOPHASE 30 5000000 UUUU..."
//   Java answers:  "7 {"success":true, ...}"      (the number 7 is the request id)
export class JavaBridge {

    constructor({ classPath, javaCommand = 'java', timeoutMs = 60000 }) {

        this.classPath = classPath;
        this.javaCommand = javaCommand;
        this.timeoutMs = timeoutMs;
        this.process = null;
        this.startPromise = null;
        this.waiting = new Map(); // request id -> { resolve, reject, timer }
        this.nextId = 1;

    }

    // Starts Java if it is not running yet.
    start() {
        if (this.startPromise) {
            return this.startPromise;
        }

        this.startPromise = new Promise((resolve, reject) => {
            const child = spawn(this.javaCommand, ['-Xmx1g', '-cp', this.classPath, 'com.example.rubik.SolverServer']);
            this.process = child;

            child.on('error', (error) => {
                this.reset(new HttpError(503, `Cannot start Java (${error.message}). Is Java installed and the solver compiled?`));
                reject(new HttpError(503, `Cannot start Java (${error.message}). Is Java installed and the solver compiled?`));
            });

            child.on('exit', () => {
                this.reset(new HttpError(503, 'The Java solver stopped. It will be restarted on the next request.'));
                reject(new HttpError(503, 'The Java solver stopped before it was ready. Did you compile it? (see README)'));
            });

            child.stderr.on('data', (data) => console.error(`[java] ${data}`.trim()));

            const lines = readline.createInterface({ input: child.stdout });
            lines.on('line', (line) => {
                if (line === 'READY') {
                    resolve();
                } else {
                    this.handleReply(line);
                }
            });
        });

        return this.startPromise;
    }

    // Sends one command and waits for the JSON answer.
    async send(command) {
        await this.start();

        const id = this.nextId++;
        return new Promise((resolve, reject) => {
            const timer = setTimeout(() => {
                this.stop();
                reject(new HttpError(504, 'The Java solver took too long and was stopped. Try smaller limits.'));
            }, this.timeoutMs);

            this.waiting.set(id, { resolve, reject, timer });
            this.process.stdin.write(`${id} ${command}\n`);
        });
    }

    // Called for every answer line from Java.
    handleReply(line) {
        const space = line.indexOf(' ');
        const id = Number(line.slice(0, space));
        const request = this.waiting.get(id);
        if (!request) {
            return;
        }

        this.waiting.delete(id);
        clearTimeout(request.timer);
        try {
            request.resolve(JSON.parse(line.slice(space + 1)));
        } catch (error) {
            request.reject(new HttpError(500, 'The Java solver sent an answer that is not valid JSON'));
        }
    }

    // Forget the dead process and fail everything that was waiting.
    reset(error) {
        this.process = null;
        this.startPromise = null;
        for (const request of this.waiting.values()) {
            clearTimeout(request.timer);
            request.reject(error);
        }
        this.waiting.clear();
    }

    stop() {
        if (this.process) {
            this.process.kill();
        }
    }
}

// Sends a command and turns Java's "bad input" answers into HTTP 400 errors.
export async function askJava(bridge, command) {

    const reply = await bridge.send(command);

    if (reply.kind === 'invalid') {

        throw new HttpError(400, reply.error);

    }

    if (reply.kind === 'internal') {

        throw new HttpError(500, reply.error);

    }

    return reply;
}