import static java.lang.System.out;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import agents.ArtificialAgent;
import game.actions.EDirection;
import game.actions.slim.SAction;
import game.actions.slim.SMove;
import game.actions.slim.SPush;
import game.board.compact.BoardCompact;
import game.board.slim.BoardSlim;
import game.board.slim.STile;


/**
 * A* Agent
 *
 * @author Alperen Guncan
 */
public class MyAgent extends ArtificialAgent {
    // Board that is currently being solved
    protected static BoardSlim board;
    // Counter of searched nodes
    protected int searchedNodes;
    // Higher dimension of board -> used for boxes pos bitmask (to avoid collisions)
    protected static int dim;
    // Goal pos
    private List<Point> goals;
    // Composition with DeadSquareDetector
    private DeadSquareDetector dsd;
    // Zobrist hashes -> Random Long for each position used for state hashing
    private static Long[][][] zobrist_hashes;

    @Override
    protected List<EDirection> think(BoardCompact board) {
        MyAgent.board = board.makeBoardSlim();
        searchedNodes = 0;
        // Find longer side for avoiding collisions on box bitmask
        dim = Math.max(board.width(), board.height());
        goals = findGoals(MyAgent.board);
        dsd = new DeadSquareDetector(MyAgent.board);
        // Initialize Zobrist hashtable, 0 -> boxes, 1 -> player
        zobrist_hashes = new Long[2][board.width()][board.height()];
        Random rand = new Random();
        for (int i = 0; i < board.width(); i++)
            for (int j = 0; j < board.height(); j++) {
                zobrist_hashes[0][i][j] = rand.nextLong();
                zobrist_hashes[1][i][j] = rand.nextLong();
            }
        // Start from 1 (or higher) for less risky solver
        int corralRisk = 0, corralStep = 1, maxCost = 500;
        long searchStartMillis = System.currentTimeMillis();
        while (corralRisk <= MyAgent.board.boxCount) {
            // Clear cache from old corrals
            dsd.corralCache = dsd.corralCache.entrySet().stream().filter(e -> !e.getValue())
                    .collect(Collectors.toMap(Map.Entry::getKey, stringBooleanEntry -> false));
            dsd.skipped = new int[]{0, 0, 0};
            dsd.corralRisk = corralRisk;
            List<EDirection> result = a_star(maxCost, corralRisk); // depth of search tree
            corralRisk += corralStep;
            if (result == null) continue;
            long searchTime = System.currentTimeMillis() - searchStartMillis;
            if (verbose) {
                out.println("Nodes visited: " + searchedNodes);
                out.printf("Performance: %.1f nodes/sec\n",
                        ((double) searchedNodes / (double) searchTime * 1000));
            }
            if (!result.isEmpty()) return result;
        }
        return null;
    }

    private List<EDirection> a_star(int maxCost, int corralRisk) {
        // Initialize
        boolean completed = false;
        long completedHash = goals.stream().map(g -> zobrist_hashes[0][g.x][g.y]).reduce(0L, (a, e) -> a ^ e);
        BitSet boxes = new BitSet(board.width() * board.height());
        for (Point b : findBoxes(board)) boxes.set(b.x * dim + b.y);
        // Action placeholder, is ignored anyway
        Node start = new Node(boxes, null, SPush.getAction(EDirection.RIGHT), 0, manhattanH(boxes),
                board.playerX, board.playerY);
        // Heuristic is consistent + uniform costs -> first reach is optimal (set sufficient)
        Set<Long> vis = new HashSet<>();
        vis.add(start.hashFull);
        Queue<Node> q = new PriorityQueue<>();
        q.add(start);
        // A*
        Node curr = null;
        while (!q.isEmpty()) {
            curr = q.poll();
            searchedNodes++;
            // Guard clauses
            completed = curr.hashBox == completedHash;
            // Heuristic is admissible - first goal reach is optimal
            if (completed) break;
            if (curr.g > maxCost) continue;
            List<SAction> actions = new ArrayList<>(4);
            // Add possible moves
            EDirection opposite = curr.pa.getDirection().opposite();
            for (SMove move : SMove.getActions()) {
                EDirection dir = move.getDirection();
                int nextX = curr.playerX + dir.dX, nextY = curr.playerY + dir.dY;
                // Next square is free (no wall)
                if ((board.tiles[nextX][nextY] & STile.WALL_FLAG) == 0 &&
                        // Player does not backtrack
                        !(curr.pa instanceof SMove && dir.equals(opposite))
                        // Next square is free (no box) -> put here because second condition is more restricting
                        && !curr.boxes.get(nextX * dim + nextY))
                    actions.add(move);
            }
            // Add possible pushes
            for (SPush push : SPush.getActions()) {
                EDirection dir = push.getDirection();
                int nextX = curr.playerX + dir.dX, nextY = curr.playerY + dir.dY;
                int nextXX = nextX + dir.dX, nextYY = nextY + dir.dY;
                // Next square has box, and next-next square is free (no wall or box)
                if (curr.boxes.get(nextX * dim + nextY) && (board.tiles[nextXX][nextYY] & STile.WALL_FLAG) == 0 &&
                        !curr.boxes.get(nextXX * dim + nextYY))
                    actions.add(push);
            }
            // For each possible action
            for (SAction action : actions) {
                Node next = curr.copy(action);
                EDirection dir = action.getDirection();
                int nextX = next.playerX + dir.dX, nextY = next.playerY + dir.dY;
                // Slight code repetition because pushes have a lot of extra logic
                if (action instanceof SPush) {
                    int nextXX = nextX + dir.dX, nextYY = nextY + dir.dY;
                    next.moveBox(nextX, nextY, nextXX, nextYY);
                    next.movePlayer(nextX, nextY);
                    if (vis.contains(next.hashFull) || dsd.detectSimple(nextXX, nextYY)
                            || dsd.detectFreeze(next.boxes, nextXX, nextYY, next.hashBox))
//                            || dsd.detectCorral(next.boxes, nextXX, nextYY, dir.dX, dir.dY, next.playerX,
//                        next.playerY, next.hashFull))
                        continue;
                    next.h = manhattanH(next.boxes);
                } else {
                    // SMove -> boxes unchanged -> redundant deadlock detection and heuristic recalculation
                    next.movePlayer(nextX, nextY);
                    if (vis.contains(next.hashFull)) continue;
                }
                vis.add(next.hashFull);
                q.add(next);
            }
        }
        // Backtracking to build action chain
        if (curr == null || !completed) return null;
        List<EDirection> actions = new LinkedList<>();
        while (curr.parent != null) {
            actions.add(0, curr.pa.getDirection());
            curr = curr.parent;
        }
        System.out.print(Arrays.stream(dsd.skipped).mapToObj(i -> i + " ").reduce("", String::concat));
        System.out.println("risk: " + corralRisk);
        System.out.println(actions.stream().map(o -> o.toString().substring(0, 1)).collect(Collectors.joining()));
        return actions;
    }

    // Heuristic function - manhattan closest matching
    private float manhattanH(BitSet boxes) {
        return boxes.stream().map(b -> {
            int bX = b / dim, bY = b % dim;
            return goals.stream().map(g -> Math.abs(g.x - bX) + Math.abs(g.y - bY))
                    .reduce(Math::min).orElse(0);
        }).reduce(Integer::sum).orElse(0);
    }

    // State
    static class Node implements Comparable<Node> {
        BitSet boxes;
        Node parent;
        SAction pa;
        int playerX, playerY, g;
        long hashBox, hashFull;
        float h;

        public Node(BitSet boxes, Node parent, SAction pa, int g, float h, int playerX, int playerY) {
            this(boxes, parent, pa, g, h, playerX, playerY, 0L, 0L);
            boxes.stream().forEach(e -> this.hashBox ^= zobrist_hashes[0][e / dim][e % dim]);
            this.hashFull = this.hashBox;
            this.hashFull ^= zobrist_hashes[1][playerX][playerY];
        }

        public Node(BitSet boxes, Node parent, SAction pa, int g, float h, int playerX, int playerY, Long hashBox,
                    Long hashFull) {
            this.boxes = boxes;
            this.parent = parent;
            this.pa = pa;
            this.g = g;
            this.h = h;
            this.playerX = playerX;
            this.playerY = playerY;
            this.hashBox = hashBox;
            this.hashFull = hashFull;
        }

        public Node copy(SAction pa) {
            return new Node((BitSet) boxes.clone(), this, pa, g + 1, h, playerX, playerY, hashBox, hashFull);
        }

        public void movePlayer(int tx, int ty) {
            playerX = tx;
            playerY = ty;
            hashFull = hashBox;
            hashFull ^= zobrist_hashes[1][tx][ty];
        }

        public void moveBox(int x, int y, int tx, int ty) {
            boxes.clear(x * dim + y);
            boxes.set(tx * dim + ty);
            hashBox ^= zobrist_hashes[0][x][y];
            hashBox ^= zobrist_hashes[0][tx][ty];
        }

        public int compareTo(Node o) {
            return Float.compare(this.g + this.h, o.g + o.h);
        }
    }

    // Point DAO
    static public class Point {
        int x, y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    // Class for finding dead squares
    static class DeadSquareDetector {
        // Static dead square positions
        boolean[][] dead;
        // Skipped states counter for different deadlock types, and directions helper (used for flooding)
        int[] skipped = new int[]{0, 0, 0}, dirs = new int[]{-1, 0, 1, 0};
        // 'Global' variables/counters visible to all recursive stacks for corral deadlock detection
        boolean corral = true;
        int corralRisk = 0, corralBoxes = 0, corralGoals = 0;
        // Caches for freeze and corral deadlocks
        Map<Long, Boolean> freezeCache = new HashMap<>(), corralCache = new HashMap<>();

        public DeadSquareDetector(BoardSlim board) {
            this.dead = this.detectSimple(board);
        }

        // Detect simple deadlocks (static) - tiles from which a box cannot move, independent of other boxes
        public boolean[][] detectSimple(BoardSlim board) {
            boolean[][] res = new boolean[board.width()][board.height()];
            // Flood fill for dead squares, starting from each goal
            for (Point goal : findGoals(board)) pull(board, res, goal.x, goal.y);
            // Invert result (not visited -> dead)
            for (int i = 0; i < board.width(); i++) for (int j = 0; j < board.height(); j++) res[i][j] ^= true;
            return res;
        }

        private boolean detectSimple(int x, int y) {
            boolean res = dead[x][y];
            if (res) this.skipped[0]++;
            return res;
        }

        private void pull(BoardSlim board, boolean[][] res, int x, int y) {
            res[x][y] = true;
            for (int i = 0; i < 4; i++) {
                int nx = x + dirs[i], ny = y + dirs[(i + 1) % 4];
                // Check bounds
                if (nx < 1 || ny < 1 || nx > res.length - 2 || ny > res[0].length - 2) continue;
                // Check that it can be pulled (two non-walls in the direction)
                if (res[nx][ny] || STile.isWall(board.tiles[nx][ny]) ||
                        STile.isWall(board.tiles[nx + dirs[i]][ny + dirs[(i + 1) % 4]])) continue;
                pull(board, res, nx, ny);
            }
        }

        // Detect freeze deadlocks (dynamic) - tiles from which a box cannot move, depends on other boxes
        public boolean detectFreeze(BitSet boxes, int x, int y, Long hash) {
            // Return cached config if possible
            if (freezeCache.containsKey(hash)) return freezeCache.get(hash);
            Set<Integer> frozen = new HashSet<>();
            // Get all frozen blocks in curr config
            detectFreeze(boxes, x, y, frozen, new BitSet(board.width() * dim + board.height()));
            // If any frozen block is not on goal -> dead state
            boolean res = frozen.stream().anyMatch(b -> (STile.PLACE_FLAG & board.tiles[b / dim][b % dim]) == 0);
            if (res) this.skipped[1]++;
            freezeCache.put(hash, res);
            return res;
        }

        private boolean detectFreeze(BitSet boxes, int x, int y, Set<Integer> f, BitSet bs) {
            // Check if frozen in x- and y-axis
            boolean[] frozen = new boolean[2];
            for (int i = 0; i < 2; i++) {
                int dx = x + dirs[i], dy = y + dirs[i + 1], ddx = x + dirs[i + 2], ddy = y + dirs[(i + 3) % 4];
                // Check for an axis if there's 1 wall, or 2 dead states
                frozen[i] = STile.isWall(board.tiles[dx][dy]) || STile.isWall(board.tiles[ddx][ddy]) ||
                        bs.get(dim * dx + dy) || bs.get(dim * ddx + ddy) || (dead[dx][dy] && dead[ddx][ddy]);
            }
            // If frozen from both axes - short-circuit guard
            if (frozen[0] && frozen[1]) {
                f.add(x * dim + y);
                return true;
            }
            for (int i = 0; i < 2; i++)
                if (frozen[i]) {
                    // Prevent circular check
                    bs.set(x * dim + y);
                    int dy = y + dirs[i], dx = x + dirs[i + 1];
                    // If box -> recursively check if next box is frozen
                    if (boxes.get(dx * dim + dy) && !bs.get(dx * dim + dy))
                        frozen[1 - i] = detectFreeze(boxes, dx, dy, f, bs);
                    // Short-circuit guard
                    if (frozen[1 - i]) break;
                    int ddy = y + dirs[i + 2], ddx = x + dirs[(i + 3) % 4];
                    if (boxes.get(ddx * dim + ddy) && !bs.get(ddx * dim + ddy))
                        frozen[1 - i] = detectFreeze(boxes, ddx, ddy, f, bs);
                    // Short-circuit guard
                    if (frozen[1 - i]) break;
                }
            // If frozen from both axes
            if (frozen[0] && frozen[1]) {
                f.add(x * dim + y);
                return true;
            }
            return false;
        }

        @SuppressWarnings("unused")
        // Detect corral deadlocks - when pushed box forms a closed unreachable area with not enough goals
        public boolean detectCorral(BitSet boxes, int x, int y, int dx, int dy, int pX, int pY, Long hash) {
            if (corralRisk == board.boxCount) return false;
            // Return cached config if possible
            if (corralCache.containsKey(hash)) return corralCache.get(hash);
            // If neighboring tiles are not obstacles, cannot form a coral
            if ((board.tiles[x - dy][y - dx] & STile.WALL_FLAG) == 0 ||
                    (board.tiles[x + dy][y + dx] & STile.WALL_FLAG) == 0) return false;
            if ((board.tiles[x + dx][y + dy] & STile.WALL_FLAG) != 0) return false;
            corral = true;
            corralBoxes = corralGoals = 0;
            floodFill(boxes, x + dx, y + dy, pX, pY, new boolean[board.width()][board.height()]);
            // If player or enough goals inside coral, then may be solvable
            boolean res = corral && corralGoals + corralRisk < corralBoxes;
            if (res) this.skipped[2]++;
            corralCache.put(hash, res);
            return res;
        }

        private void floodFill(BitSet boxes, int x, int y, int playerX, int playerY, boolean[][] seen) {
            seen[x][y] = true;
            // Check for player
            if (playerX == x && playerY == y) {
                corral = false;
                return;
            }
            // Check for goal
            if ((board.tiles[x][y] & STile.PLACE_FLAG) != 0) corralGoals++;
            // Check that it is not a wall
            if ((board.tiles[x][y] & STile.WALL_FLAG) != 0) return;
            // Check for box
            if (boxes.get(x * dim + y)) {
                corralBoxes++;
                return;
            }
            // Continue flooding
            for (int i = 0; i < 4; i++) {
                // Check bounds and unvisited
                int nx = x + dirs[i], ny = y + dirs[(i + 1) % 4];
                if (nx == 0 || ny == 0 || nx == seen.length - 1 || ny == seen[0].length - 1) continue;
                if (seen[nx][ny] || !corral) continue;
                floodFill(boxes, nx, ny, playerX, playerY, seen);
            }
        }
    }

    // Helper for finding all goals in a board
    private static List<Point> findGoals(BoardSlim board) {
        List<Point> res = new ArrayList<>();
        for (int i = 1; i < board.width() - 1; i++)
            for (int j = 1; j < board.height() - 1; j++)
                if ((STile.PLACE_FLAG & board.tiles[i][j]) != 0) res.add(new Point(i, j));
        return res;
    }

    // Helper for finding all boxes in a board
    public static Point[] findBoxes(BoardSlim board) {
        List<Point> res = new ArrayList<>();
        for (int i = 1; i < board.width() - 1; i++)
            for (int j = 1; j < board.height() - 1; j++)
                if ((STile.BOX_FLAG & board.tiles[i][j]) != 0) res.add(new Point(i, j));
        return res.toArray(Point[]::new);
    }
}
