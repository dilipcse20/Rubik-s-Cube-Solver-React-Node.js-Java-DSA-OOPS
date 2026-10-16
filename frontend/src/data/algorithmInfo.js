// Text shown in the "Algorithm Explanation" section.
// The limits below were measured on a normal laptop-class machine
// with the default settings. Your times will differ.

export const ALGORITHMS = [
  { value: 'BFS', label: 'BFS (Breadth-First Search)' },
  { value: 'DFS', label: 'DFS (Depth-Limited DFS)' },
  { value: 'IDDFS', label: 'IDDFS (Iterative Deepening DFS)' },
  { value: 'IDASTAR', label: 'IDA* (Iterative Deepening A*)' },
  { value: 'TWOPHASE', label: 'Two-Phase (solves ANY cube)' },
];

export const ALGORITHM_INFO = {
  BFS: {
    name: 'Breadth-First Search (BFS)',
    howItWorks: [
      'Start from the scrambled cube.',
      'Try all 18 moves to get every state 1 move away, then every state 2 moves away, and so on.',
      'A visited set remembers states already seen, so none is explored twice.',
      'The first time the solved cube appears, the path to it is a shortest solution.',
    ],
    time: 'O(b^d) where b is the branching factor (up to 18) and d is the solution length',
    space: 'O(b^d) because every seen state is stored',
    advantages: [
      'Always finds a shortest solution (in face turns).',
      'Easy to understand.',
    ],
    limitations: [
      'Memory grows extremely fast. This project stops at 500,000 stored states.',
      'In practice it is reliable up to about 4 moves.',
    ],
  },
  DFS: {
    name: 'Depth-Limited Depth-First Search (DFS)',
    howItWorks: [
      'Follow one branch of moves down to the depth limit, then back up and try the next branch.',
      'The depth limit stops it from wandering forever.',
      'A map of "state -> shallowest depth seen" prevents repeating states.',
      'It returns the first solution it finds.',
    ],
    time: 'O(b^d) in the worst case',
    space: 'O(number of stored states), capped at 500,000 here',
    advantages: [
      'Can find a solution quickly when it happens to go the right way.',
      'Shows clearly why a depth limit and a visited set are needed.',
    ],
    limitations: [
      'Does NOT guarantee a shortest solution. It often returns a longer one.',
      'It may use up its state limit exploring a deep wrong branch first.',
      'In practice it is reliable up to about 5 moves.',
    ],
  },
  IDDFS: {
    name: 'Iterative Deepening DFS (IDDFS)',
    howItWorks: [
      'Run a DFS that may go 1 move deep. If no solution, run it again allowing 2 moves, then 3, and so on.',
      'Shallow depths are always finished first, so the first solution found is a shortest one.',
      'Only the current path is kept in memory.',
    ],
    time: 'O(b^d). Repeating the shallow levels costs little because the last level is by far the largest',
    space: 'O(d) since only the current path is stored',
    advantages: [
      'Shortest solution like BFS, but with tiny memory use.',
      'No visited set needed.',
    ],
    limitations: [
      'Time still grows about 15x with every extra move.',
      'In practice it is reliable up to about 6 moves (a few million states).',
    ],
  },
  IDASTAR: {
    name: 'Iterative Deepening A* (IDA*)',
    howItWorks: [
      'g(n) = moves made so far. h(n) = estimate of moves still needed. f(n) = g(n) + h(n).',
      'h(n) counts corners and edges that are not home: one move touches only 4 corners and 4 edges, so it needs at least ceil(wrong / 4) moves.',
      'This estimate never overestimates, so it is admissible and the first solution is still a shortest one.',
      'Any state with f(n) above the current threshold is pruned. If nothing is found, the threshold grows and the search repeats.',
    ],
    time: 'O(b^d) worst case; pruning makes it much faster than IDDFS on shallow scrambles',
    space: 'O(d) since only the current path is stored',
    advantages: [
      'Shortest solution with tiny memory use.',
      'Pruning explores far fewer states than IDDFS.',
    ],
    limitations: [
      'The heuristic is weak (never more than 3), so deep scrambles are still out of reach.',
      'In practice it is reliable up to about 8 moves. For deeper scrambles use Two-Phase.',
    ],
  },
  TWOPHASE: {
    name: 'Two-Phase Solver (Kociemba-style)',
    howItWorks: [
      'Phase 1: using all 18 moves, reach a "half-solved" group where every corner is untwisted, every edge is unflipped and the 4 middle-layer edges are in the middle layer.',
      'Phase 2: finish the cube using only the 10 moves that stay inside that group (U, D and the half turns R2, L2, F2, B2).',
      'Each phase is an IDA* search. It looks at small numbers (coordinates such as "corner twist") instead of the whole cube.',
      'Tables built once give the move result for each coordinate (move tables) and the exact moves needed to fix it (prune tables = the heuristic).',
    ],
    time: 'Very fast in practice: about 20-30 moves in milliseconds after the tables are built (about half a second, once)',
    space: 'A few MB for the tables',
    advantages: [
      'Solves ANY legal scramble, even a fully mixed cube.',
      'Works for uploaded cubes too.',
    ],
    limitations: [
      'The solution is short (usually 20-26 moves) but NOT guaranteed to be the shortest.',
      'No symmetry tricks and no search for improvements, so it is simpler (and a few moves longer) than the best solvers.',
      'Impossible cubes (a twisted corner, a flipped edge, two swapped pieces) are rejected.',
    ],
  },
};
