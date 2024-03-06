import static java.lang.System.out;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.stream.Collectors;

import agents.ArtificialAgent;
import game.actions.EDirection;
import game.board.compact.BoardCompact;
import game.board.slim.BoardSlim;
import game.board.slim.STile;


public class MyAgent extends ArtificialAgent {
    // Board that is currently being solved
    protected static BoardSlim board;
    // Array for walls
    protected static boolean[][] walls;
    // Array containing distance between tile and closest goal, and helper dirs array
    protected static int[][] minDists;
    // Dir helper for picking directions cleanly
    protected static int[] dirs = new int[]{-1, 0, 1, 0};
    // Counter of searched nodes
    protected int searchedNodes;
    // Goal positions
    private List<Point> goals;
    // Composition with DeadSquareDetector
    private DeadSquareDetector dsd;
    // Zobrist hashes -> Random Long for each position used for state hashing
    private static long[][][] zobrist_hashes;

    @Override
    protected List<EDirection> think(BoardCompact origBoard) {
        board = origBoard.makeBoardSlim();
        searchedNodes = 0;
        goals = findEntities(board, STile.PLACE_FLAG);
        dsd = new DeadSquareDetector(board);
        // Initialize Zobrist hashtable, 0 -> boxes, 1 -> player
        zobrist_hashes = new long[2][board.width()][board.height()];
        walls = new boolean[board.width()][board.height()];
        Random rand = new Random();
        for (int i = 0; i < board.width(); i++)
            for (int j = 0; j < board.height(); j++) {
                if (STile.isWall(board.tiles[i][j])) walls[i][j] = true;
                zobrist_hashes[0][i][j] = rand.nextLong();
                zobrist_hashes[1][i][j] = rand.nextLong();
            }
        calculateMinDistTable();
        long searchStartMillis = System.currentTimeMillis();
        List<EDirection> result = a_star();
        long searchTime = System.currentTimeMillis() - searchStartMillis;
        if (verbose) {
            out.println("Nodes visited: " + searchedNodes);
            out.printf("Performance: %.1f nodes/sec\n",
                    ((double) searchedNodes / (double) searchTime * 1000));
        }
        return result;
    }

    private List<EDirection> a_star() {
        // Initialize
        boolean completed = false;
        long completedHash = goals.stream().map(g -> zobrist_hashes[0][g.x][g.y]).reduce(0L, (a, e) -> a ^ e);
        short[] boxes = new short[board.boxCount];
        int startH = 0, idx = 0;
        for (Point box : findEntities(board, STile.BOX_FLAG)) {
            boxes[idx++] = (short) ((box.x & 0xFF) | (box.y & 0xFF) << 8);
            startH += minDists[box.x][box.y];
        }
        // Action placeholder, is ignored anyway
        State start = new State(boxes, null, EDirection.NONE, startH, board.playerX, board.playerY);
        // Heuristic is consistent + uniform costs -> first reach is optimal (set sufficient)
        LongHashMap vis = new LongHashMap();
        vis.put(start.hashFull);
        PriorityQueue<State> q = new PriorityQueue<>(List.of(start));
        // A*
        State curr = null;
        EDirection[] dirList = {EDirection.UP, EDirection.RIGHT, EDirection.DOWN, EDirection.LEFT};
        while (!q.isEmpty()) {
            curr = q.remove();
            searchedNodes++;
            // Guard clauses
            completed = curr.hashBox == completedHash;
            // Heuristic is admissible - first goal reach is optimal
            if (completed) break;
            for (int i = 0; i < 4; i++) {
                EDirection dir = dirList[i];
                int nextX = curr.playerX + dir.dX, nextY = curr.playerY + dir.dY;
                // If there's a wall, skip direction
                if (walls[nextX][nextY]) continue;
                // If there's no box, we can move there
                if (!boxAt(curr.boxes, nextX, nextY)) {
                    long nextHashFull = curr.hashBox;
                    nextHashFull ^= zobrist_hashes[1][nextX][nextY];
                    // Check if state visited
                    if (vis.contains(nextHashFull)) continue;
                    // SMove -> boxes unchanged -> redundant deadlock detection, box cloning, and heuristic recalculation
                    State next = curr.copy(dir, curr.boxes, curr.hashBox, nextHashFull, nextX, nextY);
                    vis.put(nextHashFull);
                    q.add(next);
                }
                // Possible candidate for pushing there
                else {
                    int nextXX = nextX + dir.dX, nextYY = nextY + dir.dY;
                    // Future box position is not free (wall or box)
                    if (walls[nextXX][nextYY] || boxAt(curr.boxes, nextXX, nextYY)) continue;
                    // Dead future box position
                    if (dsd.detectSimple(nextXX, nextYY)) continue;
                    // Update hashes separately for performance
                    long nextHashFull = curr.hashBox;
                    nextHashFull ^= zobrist_hashes[0][nextX][nextY];
                    nextHashFull ^= zobrist_hashes[0][nextXX][nextYY];
                    long nextHashBox = nextHashFull;
                    nextHashFull ^= zobrist_hashes[1][nextX][nextY];
                    // Check if state visited
                    if (vis.contains(nextHashFull)) continue;
                    // Copy and update boxes
                    short[] nextBoxes = Arrays.copyOf(curr.boxes, board.boxCount);
                    for (int ib = 0; ib < nextBoxes.length; ib++) {
                        short b = nextBoxes[ib];
                        if (nextX == (b & 0xFF) && nextY == ((b >> 8) & 0xFF)) {
                            nextBoxes[ib] = (short) ((nextXX & 0xFF) | (nextYY & 0xFF) << 8);
                            break;
                        }
                    }
                    // Check dynamic deadlock
                    if (dsd.detectFreeze(nextBoxes, nextXX, nextYY, nextHashBox)) continue;
                    State next = curr.copy(dir, nextBoxes, nextHashBox, nextHashFull, nextX, nextY);
                    // Update heuristic
                    next.f = next.f - minDists[nextX][nextY] + minDists[nextXX][nextYY];
                    vis.put(nextHashFull);
                    q.add(next);
                }
            }
        }
        // Backtracking to build action chain
        if (curr == null || !completed) return null;
        List<EDirection> actions = new LinkedList<>();
        while (curr.parent != null) {
            actions.add(0, curr.pa);
            curr = curr.parent;
        }
        if (verbose) {
            out.print(Arrays.stream(dsd.skipped).mapToObj(i -> i + " ").reduce("", String::concat));
            out.println(actions.stream().map(o -> o.toString().substring(0, 1)).collect(Collectors.joining()));
        }
        return actions;
    }

    private void calculateMinDistTable() {
        minDists = new int[board.width()][board.height()];
        for (int[] minDist : minDists) Arrays.fill(minDist, Integer.MAX_VALUE);
        for (Point g : goals) {
            int c = 0;
            Queue<Point> q = new ArrayDeque<>(List.of(g));
            List<Integer> vis = new ArrayList<>(List.of(g.x * board.height() + g.y));
            while (true) {
                Queue<Point> qq = new ArrayDeque<>();
                while (!q.isEmpty()) {
                    Point curr = q.remove();
                    minDists[curr.x][curr.y] = Math.min(minDists[curr.x][curr.y], c);
                    for (int i = 0; i < 4; i++) {
                        Point next = new Point(curr.x + (dirs[i]), curr.y + dirs[(i + 1) % 4]);
                        if (vis.contains(next.x * board.height() + next.y) || walls[next.x][next.y]) continue;
                        vis.add(next.x * board.height() + next.y);
                        qq.add(next);
                    }
                }
                if (qq.isEmpty()) break;
                q.addAll(qq);
                c++;
            }
        }
    }

    static class State implements Comparable<State> {
        short[] boxes;
        State parent;
        EDirection pa;
        int playerX, playerY, f;
        long hashBox, hashFull;

        public State(short[] boxes, State parent, EDirection pa, int f, int playerX, int playerY) {
            this(boxes, parent, pa, f, playerX, playerY, 0L, 0L);
            for (short b : boxes) this.hashBox ^= zobrist_hashes[0][b & 0xFF][(b >> 8) & 0xFF];
            this.hashFull = this.hashBox;
            this.hashFull ^= zobrist_hashes[1][playerX][playerY];
        }

        public State(short[] boxes, State parent, EDirection pa, int f, int playerX, int playerY,
                     long hashBox, long hashFull) {
            this.boxes = boxes;
            this.parent = parent;
            this.pa = pa;
            this.f = f;
            this.playerX = playerX;
            this.playerY = playerY;
            this.hashBox = hashBox;
            this.hashFull = hashFull;
        }

        public State copy(EDirection dir, short[] boxes, long hashBox, long hashFull, int px, int py) {
            return new State(boxes, this, dir, f + 1, px, py, hashBox, hashFull);
        }

        public int compareTo(State o) {
            return Integer.compare(this.f, o.f);
        }
    }

    // Class for finding dead squares
    static class DeadSquareDetector {
        // Static dead square positions
        boolean[][] dead;
        // Skipped states counter for different deadlock types, and directions helper (used for flooding)
        int[] skipped = new int[]{0, 0};
        // Caches for freeze and corral deadlocks
        Map<Long, Boolean> freezeCache = new HashMap<>();

        public DeadSquareDetector(BoardSlim board) {
            this.dead = this.detect(board);
        }

        // Detect simple deadlocks (static) - tiles from which a box cannot move, independent of other boxes
        public boolean[][] detect(BoardSlim board) {
            boolean[][] res = new boolean[board.width()][board.height()];
            // Flood fill for dead squares, starting from each goal
            for (Point goal : findEntities(board, STile.PLACE_FLAG)) pull(board, res, goal.x, goal.y);
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
        public boolean detectFreeze(short[] boxes, int x, int y, long hash) {
            // Return cached config if possible
            Boolean c = freezeCache.get(hash);
            if (c != null) {
                this.skipped[1]++;
                return c;
            }
            List<Integer> frozen = new ArrayList<>();
            // Get all frozen blocks in curr config
            detectFreeze(boxes, x, y, frozen, new short[boxes.length], 0);
            // If any frozen block is not on goal -> dead state
            boolean res = frozen.stream().anyMatch(b ->
                    (STile.PLACE_FLAG & board.tiles[b / board.height()][b % board.height()]) == 0);
            if (res) this.skipped[1]++;
            freezeCache.put(hash, res);
            return res;
        }

        private boolean detectFreeze(short[] boxes, int x, int y, List<Integer> f, short[] bs, int ii) {
            // Check if frozen in x- and y-axis
            boolean[] frozen = new boolean[2];
            for (int i = 0; i < 2; i++) {
                int dx = x + dirs[i], dy = y + dirs[i + 1], ddx = x + dirs[i + 2], ddy = y + dirs[(i + 3) % 4];
                // Check for an axis if there's 1 wall, or 2 dead states
                frozen[i] = walls[dx][dy] || walls[ddx][ddy] || boxAt(bs, dx, dy) || boxAt(bs, ddx, ddy) ||
                        (dead[dx][dy] && dead[ddx][ddy]);
            }
            // If frozen from both axes - short-circuit guard
            if (frozen[0] && frozen[1]) {
                f.add(x * board.height() + y);
                return true;
            }
            for (int i = 0; i < 2; i++)
                if (frozen[i]) {
                    // Prevent circular check
                    bs[ii++] = (short) ((x & 0xFF) | (y & 0xFF) << 8);
                    int dy = y + dirs[i], dx = x + dirs[i + 1];
                    // If box -> recursively check if next box is frozen
                    if (boxAt(boxes, dx, dy) && !boxAt(bs, dx, dy))
                        frozen[1 - i] = detectFreeze(boxes, dx, dy, f, bs, ii);
                    // Short-circuit guard
                    if (frozen[1 - i]) break;
                    int ddy = y + dirs[i + 2], ddx = x + dirs[(i + 3) % 4];
                    if (boxAt(boxes, ddx, ddy) && !boxAt(bs, ddx, ddy))
                        frozen[1 - i] = detectFreeze(boxes, ddx, ddy, f, bs, ii);
                    // Short-circuit guard
                    if (frozen[1 - i]) break;
                }
            // If frozen from both axes
            if (frozen[0] && frozen[1]) {
                f.add(x * board.height() + y);
                return true;
            }
            return false;
        }
    }

    // Helper for finding entities in a board (goals/boxes/walls/etc.)
    private static List<Point> findEntities(BoardSlim board, byte flag) {
        List<Point> res = new ArrayList<>();
        for (int i = 1; i < board.width() - 1; i++)
            for (int j = 1; j < board.height() - 1; j++)
                if ((flag & board.tiles[i][j]) != 0) res.add(new Point(i, j));
        return res;
    }

    private static boolean boxAt(short[] boxes, int x, int y) {
        for (short b : boxes) if (x == (b & 0xFF) && y == ((b >> 8) & 0xFF)) return true;
        return false;
    }

    static public class Point {
        int x, y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    static class LongHashMap {
        private static final int DEFAULT_CAPACITY = 2_097_152;
        private static final double LOAD_FACTOR = 0.8;

        private long[] keys;
        private boolean[] occupied;
        private int size;

        public LongHashMap() {
            keys = new long[DEFAULT_CAPACITY];
            occupied = new boolean[DEFAULT_CAPACITY];
            size = 0;
        }

        private int hash(long key) {
            return (int) ((key >>> 32) % keys.length);
        }

        private void resize() {
            int newCapacity = keys.length * 2;
            long[] newKeys = new long[newCapacity];
            boolean[] newOccupied = new boolean[newCapacity];
            for (int i = 0; i < keys.length; i++) {
                if (occupied[i]) {
                    int index = hash(keys[i]);
                    while (newOccupied[index]) index = (index + 1) % newCapacity;
                    newKeys[index] = keys[i];
                    newOccupied[index] = true;
                }
            }
            keys = newKeys;
            occupied = newOccupied;
        }

        public void put(long key) {
            if ((double) size / keys.length >= LOAD_FACTOR) resize();
            int index = hash(key);
            while (occupied[index]) {
                if (keys[index] == key) return;
                index = (index + 1) % keys.length;
            }
            keys[index] = key;
            occupied[index] = true;
            size++;
        }

        public boolean contains(long key) {
            int index = hash(key);
            while (occupied[index]) {
                if (keys[index] == key) return true;
                index = (index + 1) % keys.length;
            }
            return false;
        }
    }
}