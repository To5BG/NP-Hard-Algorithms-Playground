import static java.lang.System.out;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
 * The simplest Tree-DFS agent.
 *
 * @author Jimmy
 */
public class MyAgent extends ArtificialAgent {

    protected BoardSlim board;
    protected int searchedNodes;
    private List<Point> goals;
    private DeadSquareDetector dsd;
    private static Long[][][] zobrist_hashes;

    @Override
    protected List<EDirection> think(BoardCompact board) {
        this.board = board.makeBoardSlim();
        searchedNodes = 0;
        goals = findGoals(this.board);
        dsd = new DeadSquareDetector(this.board);
        init_zobrist(this.board);

        // Start from 1 (or higher) for less risky solver
        int corralRisk = 0, corralStep = 1, maxCost = 500;
        long searchStartMillis = System.currentTimeMillis();
        while (corralRisk <= this.board.boxCount) {
            // Clear cache from old corrals
            dsd.corralCache = dsd.corralCache.entrySet().stream().filter(e -> !e.getValue())
                    .collect(Collectors.toMap(Map.Entry::getKey, stringBooleanEntry -> false));
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
        // Heuristic is consistent - first reach is optimal (set sufficient)
        Set<Long> vis = new HashSet<>();
        Queue<Node> q = new PriorityQueue<>();
        BoxPoint[] boxes = findBoxes(board);
        dsd.skipped = new int[]{0, 0, 0};
        dsd.corralRisk = corralRisk;
        boolean completed = false;
//        double startH = greedyMatching(dsd.dead, boxes, goals);
        double startH = Arrays.stream(boxes).map(b -> goals.stream().map(g -> Math.abs(g.x - b.x) + Math.abs(g.y - b.y))
                .reduce(Math::min).orElse(0)).reduce(Integer::sum).orElse(0);
        Node start = new Node(boxes, board, null, null, 0, startH);
        vis.add(start.hash);
        q.add(start);
        // A*
        Node curr = null;
        while (!q.isEmpty()) {
            curr = q.poll();
            searchedNodes++;
            // Guard clauses
            completed = curr.board.isVictory();
            // Heuristic is admissible - first goal reach is optimal
            if (completed) break;
            if (curr.g > maxCost) continue;
            List<SAction> actions = new ArrayList<>(4);
            // Add possible moves
            for (SMove move : SMove.getActions())
                if (move.isPossible(curr.board) && (!(curr.pa instanceof SMove) ||
                        !move.getDirection().equals(curr.pa.getDirection().opposite()))) actions.add(move);
            // Add possible pushes
            for (SPush push : SPush.getActions()) if (push.isPossible(curr.board)) actions.add(push);
            // For each possible action
            for (SAction action : actions) {
                Node next = curr.clone();
                EDirection dir = action.getDirection();
                byte nextX = (byte) (next.board.playerX + dir.dX), nextY = (byte) (next.board.playerY + dir.dY);
                BoxPoint mBox = null;
                // Move player and, if push action, the box
                if (action instanceof SPush) mBox = next.moveBox(nextX, nextY, (byte) (nextX + dir.dX),
                        (byte) (nextY + dir.dY));
                next.movePlayer(next.board.playerX, next.board.playerY, nextX, nextY);
                int newCost = curr.g + 1;
                // Don't consider if it does not improve on previous distance or if it leads to an unsolvable position
                if (vis.contains(next.hash) || (action instanceof SPush &&
                        (dsd.detectSimple(mBox.x, mBox.y)
                                || dsd.detectFreeze(next.board, mBox.x, mBox.y, next.boxes))))
//                                || dsd.detectCorral(next.board, mBox.x, mBox.y, dir.dX, dir.dY, next.boxes))))
                    continue;
                vis.add(next.hash);
                // Update next state
                next.parent = curr;
                next.pa = action;
                next.g = newCost;
                next.h = singleH(goals, mBox, curr.h);
//                next.h = fullH(next.boxes, goals, mBox, curr.h);
                q.add(next);
            }
        }
        // Backtracking to build action chain
        if (curr == null || !completed) return null;
        List<EDirection> actions = new LinkedList<>();
        while (!curr.board.equals(board)) {
            actions.add(0, curr.pa.getDirection());
            curr = curr.parent;
        }
        System.out.print(Arrays.stream(dsd.skipped).mapToObj(i -> i + " ").reduce("", String::concat));
        System.out.println("risk: " + corralRisk);
        System.out.println(actions.stream().map(o -> o.toString().substring(0, 1)).collect(Collectors.joining()));
        return actions;
    }

    // Heuristic function - single match
    private double singleH(List<Point> goals, BoxPoint changed, double oldH) {
        // If boxes did not change, do not update
        if (changed == null) return oldH;
        // Manhattan closest matching
        double newDist = goals.stream().map(g -> Math.abs(changed.x - g.x) + Math.abs(changed.y - g.y))
                .reduce(Integer.MAX_VALUE, Integer::min);
        // Pythagorean closest matching
//        double newDist = goals.stream().map(g -> Math.sqrt(Math.pow(changed.x - g.x, 2) + Math.pow(changed.y - g.y, 2)))
//                .reduce(Double.MAX_VALUE, Double::min);
        double res = oldH - changed.dist;
        changed.dist = newDist;
        return res + changed.dist;
    }

    // Heuristic function - full match
    private double fullH(BoxPoint[] boxes, List<Point> goals, BoxPoint changed, double oldH) {
        // If boxes did not change, do not update
        if (changed == null) return oldH;
        // Greedy matching
        // If distance to best goal gets large, update matching
        Point bestGoal = goals.get(changed.closestGoalId);
        int newDist = Math.abs(bestGoal.x - changed.x) + Math.abs(bestGoal.y - changed.y);
        for (BoxPoint b : boxes) if (b.dist < newDist) return greedyMatching(boxes, goals);
        return oldH;
    }

    private void init_zobrist(BoardSlim board) {
        zobrist_hashes = new Long[2][board.width()][board.height()];
        Random rand = new Random();
        for (int i = 0; i < board.width(); i++)
            for (int j = 0; j < board.height(); j++) {
                zobrist_hashes[0][i][j] = rand.nextLong();
                zobrist_hashes[1][i][j] = rand.nextLong();
            }
    }

    // State
    static class Node implements Comparable<Node>, Cloneable {
        BoxPoint[] boxes;
        BoardSlim board;
        Node parent;
        SAction pa;
        int g;
        long hash;
        double h;

        public Node(BoxPoint[] boxes, BoardSlim board, Node parent, SAction pa, int g, double h) {
            this(boxes, board, parent, pa, g, h, 0L);
            for (BoxPoint b : boxes)
                this.hash ^= zobrist_hashes[0][b.x][b.y];
            this.hash ^= zobrist_hashes[1][board.playerX][board.playerY];
        }

        public Node(BoxPoint[] boxes, BoardSlim board, Node parent, SAction pa, int g, double h, Long hash) {
            this.boxes = boxes;
            this.board = board;
            this.parent = parent;
            this.pa = pa;
            this.g = g;
            this.h = h;
            this.hash = hash;
        }

        public Node clone() {
            BoxPoint[] newBoxes = new BoxPoint[boxes.length];
            for (int i = 0; i < boxes.length; i++) {
                BoxPoint box = boxes[i];
                newBoxes[i] = new BoxPoint(box.x, box.y, box.dist, box.closestGoalId);
            }
            return new Node(newBoxes, board.clone(), parent, pa, g, h, hash);
        }

        public void movePlayer(byte x, byte y, byte tx, byte ty) {
            board.movePlayer(x, y, tx, ty);
            hash ^= zobrist_hashes[1][x][y];
            hash ^= zobrist_hashes[1][tx][ty];
        }

        public BoxPoint moveBox(byte x, byte y, byte tx, byte ty) {
            board.moveBox(x, y, tx, ty);
            BoxPoint box = null;
            for (BoxPoint b : boxes)
                if (b.x == x && b.y == y) {
                    box = b;
                    box.x = tx;
                    box.y = ty;
                    break;
                }
            hash ^= zobrist_hashes[0][x][y];
            hash ^= zobrist_hashes[0][tx][ty];
            return box;
        }

        public boolean equals(Object c) {
            if (!(c instanceof Node)) return false;
            if (c == this) return true;
            return this.hash  == ((Node) c).hash;
        }

        public int compareTo(Node o) {
            return Double.compare(this.g + this.h, o.g + o.h);
        }
    }

    // Box point extension with two extra variables (id of closest goal, and distance to it)
    static class BoxPoint extends Point {
        double dist;
        int closestGoalId;

        public BoxPoint(int x, int y, double dist, int closestGoalId) {
            super(x, y);
            this.dist = dist;
            this.closestGoalId = closestGoalId;
        }
    }

    // Point record
    static class Point {
        int x, y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    // Class for finding dead squares
    static class DeadSquareDetector {
        boolean[][] dead;
        int[] dirs = new int[]{-1, 0, 1, 0}, skipped = new int[]{0, 0, 0};
        byte obst = STile.WALL_FLAG | STile.BOX_FLAG;
        boolean corral = true;
        int corralRisk = 0, corralBoxes = 0, corralGoals = 0;
        Map<String, Boolean> freezeCache = new HashMap<>(), corralCache = new HashMap<>();

        public DeadSquareDetector(BoardSlim board) {
            this.dead = DeadSquareDetector.detectSimple(board);
        }

        // Detect simple deadlocks (static) - tiles from which a box cannot move, independent of other boxes
        public static boolean[][] detectSimple(BoardSlim board) {
            boolean[][] res = new boolean[board.width()][board.height()];
            // Flood fill for dead squares, starting from each goal
            for (Point goal : findGoals(board)) pull(board, res, goal.x, goal.y, new int[]{0, 1, 0, -1});
            // Invert result (not visited -> dead)
            for (int i = 0; i < board.width(); i++) for (int j = 0; j < board.height(); j++) res[i][j] ^= true;
            return res;
        }

        private boolean detectSimple(int x, int y) {
            boolean res = dead[x][y];
            if (res) this.skipped[0]++;
            return res;
        }

        private static void pull(BoardSlim board, boolean[][] res, int x, int y, int[] dirs) {
            res[x][y] = true;
            for (int i = 0; i < 4; i++) {
                int nx = x + dirs[i], ny = y + dirs[(i + 1) % 4];
                // Check bounds
                if (nx < 1 || ny < 1 || nx > res.length - 2 || ny > res[0].length - 2) continue;
                // Check that it can be pulled (two non-walls in the direction)
                if (res[nx][ny] || STile.isWall(board.tiles[nx][ny]) ||
                        STile.isWall(board.tiles[nx + dirs[i]][ny + dirs[(i + 1) % 4]])) continue;
                pull(board, res, nx, ny, dirs);
            }
        }

        // Detect freeze deadlocks (dynamic) - tiles from which a box cannot move, depends on other boxes
        public boolean detectFreeze(BoardSlim board, int x, int y, BoxPoint[] boxes) {
            // Return cached config if possible
            String k = stringConfig(boxes, null);
            if (freezeCache.containsKey(k)) return freezeCache.get(k);
            Set<Point> frozen = new HashSet<>();
            // Get all frozen blocks in curr config
            detectFreeze(board.clone(), x, y, frozen);
            // If any frozen block is not on goal -> dead state
            boolean res = frozen.stream().anyMatch(b -> (STile.PLACE_FLAG & board.tiles[b.x][b.y]) == 0);
            if (res) this.skipped[1]++;
            freezeCache.put(k, res);
            return res;
        }

        private boolean detectFreeze(BoardSlim board, int x, int y, Set<Point> f) {
            // Check if frozen in x- and y-axis
            boolean[] frozen = new boolean[2];
            for (int i = 0; i < 2; i++) {
                int dx = x + dirs[i], dy = y + dirs[i + 1], ddx = x + dirs[i + 2], ddy = y + dirs[(i + 3) % 4];
                // Check for an axis if there's 1 wall, or 2 dead states
                frozen[i] = STile.isWall(board.tiles[dx][dy]) || STile.isWall(board.tiles[ddx][ddy]) ||
                        (dead[dx][dy] && dead[ddx][ddy]);
            }
            for (int i = 0; i < 2; i++)
                if (frozen[i]) {
                    // Prevent circular check
                    board.tiles[x][y] = STile.WALL_FLAG;
                    int dy = y + dirs[i], dx = x + dirs[i + 1], ddy = y + dirs[i + 2], ddx = x + dirs[(i + 3) % 4];
                    // If box -> recursively check if next box is frozen
                    if (STile.isBox(board.tiles[dx][dy])) frozen[1 - i] = detectFreeze(board, dx, dy, f);
                    if (STile.isBox(board.tiles[ddx][ddy])) frozen[1 - i] = detectFreeze(board, ddx, ddy, f);
                }
            // If frozen from both axes
            if (frozen[0] && frozen[1]) {
                f.add(new Point(x, y));
                return true;
            }
            return false;
        }

        // Detect corral deadlocks - when pushed box forms a closed unreachable area with not enough goals
        public boolean detectCorral(BoardSlim board, int x, int y, int dx, int dy, BoxPoint[] boxes) {
            if (corralRisk == board.boxCount) return false;
            // Return cached config if possible
            String k = stringConfig(boxes, null);
//            String k = stringConfig(boxes, new Point(x - dx, y - dy));
            if (corralCache.containsKey(k)) return corralCache.get(k);
            // If neighboring tiles are not obstacles, cannot form a coral
            if ((board.tiles[x - dy][y - dx] & obst) == 0 || (board.tiles[x + dy][y + dx] & obst) == 0) return false;
            if ((board.tiles[x + dx][y + dy] & obst) != 0) return false;
            corral = true;
            corralBoxes = corralGoals = 0;
            floodFill(board, x + dx, y + dy, new boolean[board.width()][board.height()]);
            // If player or enough goals inside coral, then may be solvable
            boolean res = corral && corralGoals + corralRisk < corralBoxes;
            if (res) this.skipped[2]++;
            corralCache.put(k, res);
            return res;
        }

        private void floodFill(BoardSlim board, int x, int y, boolean[][] seen) {
            seen[x][y] = true;
            // Check for player
            if (STile.isPlayer(board.tiles[x][y])) {
                corral = false;
                return;
            }
            // Check for goal
            if ((board.tiles[x][y] & STile.PLACE_FLAG) != 0) corralGoals++;
            // Check that it is not an obstacle
            if ((board.tiles[x][y] & obst) != 0) {
                if ((board.tiles[x][y] & STile.BOX_FLAG) != 0) corralBoxes++;
                return;
            }
            // Continue flooding
            for (int i = 0; i < 4; i++) {
                // Check bounds and unvisited
                int nx = x + dirs[i], ny = y + dirs[(i + 1) % 4];
                if (nx < 0 || ny < 0 || nx > seen.length - 1 || ny > seen[0].length - 1) continue;
                if (seen[nx][ny] || !corral) continue;
                floodFill(board, nx, ny, seen);
            }
        }
    }

    // Helper that matches each box to its closest goal, taking other matches into account
    // Returns heuristic (sum of Manhattan distances of matches)
    private static double greedyMatching(BoxPoint[] boxes, List<Point> goals) {
        Queue<BoxPoint> pq = new PriorityQueue<>(Comparator.comparing(a -> a.dist));
        for (int i = 0; i < boxes.length; i++)
            for (int j = 0; j < goals.size(); j++) {
                BoxPoint b = boxes[i];
                Point g = goals.get(j);
                pq.add(new BoxPoint(i, j, Math.abs(b.x - g.x) + Math.abs(b.y - g.y), -1));
            }
        int matchedBoxes = 0, matchedGoals = 0;
        double res = 0.0;
        while (!pq.isEmpty()) {
            BoxPoint curr = pq.poll();
            if (((matchedBoxes & (1 << curr.x)) != 0) || ((matchedGoals & (1 << curr.y)) != 0)) continue;
            res += curr.dist;
            boxes[curr.x].dist = curr.dist;
            boxes[curr.x].closestGoalId = curr.y;
            matchedBoxes |= 1 << curr.x;
            matchedGoals |= 1 << curr.y;
        }
        return res;
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
    private static BoxPoint[] findBoxes(BoardSlim board) {
        List<BoxPoint> res = new ArrayList<>();
        for (int i = 1; i < board.width() - 1; i++)
            for (int j = 1; j < board.height() - 1; j++)
                if ((STile.BOX_FLAG & board.tiles[i][j]) != 0) res.add(new BoxPoint(i, j, -1, -1));
        return res.toArray(BoxPoint[]::new);
    }

    // Helper for creating String concat of all boxes (and player if given) with StringBuilder
    private static String stringConfig(BoxPoint[] bps, Point playerPos) {
        StringBuilder sb = new StringBuilder();
        for (BoxPoint bp : bps) {
            sb.append(48 + bp.x);
            sb.append(',');
            sb.append(48 + bp.y);
            sb.append(',');
        }
        if (playerPos != null) {
            sb.append(48 + playerPos.x);
            sb.append(',');
            sb.append(48 + playerPos.y);
        }
        return sb.toString();
    }
}
