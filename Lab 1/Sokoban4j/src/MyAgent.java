import static java.lang.System.out;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

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

    protected List<Point> goals;

    protected int[] dirs;

    @Override
    protected List<EDirection> think(BoardCompact board) {
        this.board = board.makeBoardSlim();
        searchedNodes = 0;
        this.goals = findGoals(this.board);
        this.dirs = new int[]{-1,0,1,0};

        long searchStartMillis = System.currentTimeMillis();
        List<EDirection> result = a_star(500); // depth of search tree
        long searchTime = System.currentTimeMillis() - searchStartMillis;

        if (verbose) {
            out.println("Nodes visited: " + searchedNodes);
            out.printf("Performance: %.1f nodes/sec\n",
                    ((double) searchedNodes / (double) searchTime * 1000));
        }
        return result.isEmpty() ? null : result;
    }

    private List<EDirection> a_star(int maxCost) {
        // Initialize
        Map<Node, Integer> dist = new HashMap<>();
        Queue<Node> q = new PriorityQueue<>();
        BoxPoint[] boxes = findBoxes(board);
        Node start = new Node(boxes, board, null, null, 0, greedyMatching(boxes, goals));
        dist.put(start, 0);
        q.add(start);

        boolean[][] deadSquares = DeadSquareDetector.detectSimple(this.board);
//        System.out.println("dead squares: \n");
//        for (int y = 0 ; y < this.board.height() ; ++y) {
//            for (int x = 0 ; x < this.board.width() ; ++x)
//                System.out.print((STile.WALL_FLAG & this.board.tile(x, y)) != 0 ? '#' : (deadSquares[x][y] ? 'X' : '_'));
//            System.out.println();
//        }
        // A*
        Node curr = null;
        while (!q.isEmpty()) {
            curr = q.poll();
            searchedNodes++;
            // Guard clauses
            if (curr.board.isVictory()) break;
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
                BoxPoint movedBox = null;
                // Move player and, if push action, the box
                if (action instanceof SPush) movedBox = next.moveBox(nextX, nextY, nextX + dir.dX, nextY + dir.dY);
                next.board.movePlayer(next.board.playerX, next.board.playerY, nextX, nextY);
                int newCost = curr.g + 1;
                // Don't consider if it does not improve on previous distance or if it leads to an unsolvable position
                if (newCost + curr.h >= dist.getOrDefault(next, Integer.MAX_VALUE) || (action instanceof SPush &&
                        Arrays.stream(next.boxes).anyMatch(b -> deadSquares[b.x][b.y]))) continue;
                dist.put(next, newCost);
                // Update next state
                next.parent = curr; next.pa = action; next.g = newCost; next.h = h(boxes, goals, movedBox, curr.h);
                q.add(next);
            }
        }
        // Backtracking to build action chain
        if (curr == null) return null;
        List<EDirection> actions = new LinkedList<>();
        while (!curr.board.equals(this.board)) {
            actions.add(0, curr.pa.getDirection());
            curr = curr.parent;
        }
//        System.out.println(actions.stream().map(o -> o.toString().substring(0, 1)).collect(Collectors.joining()));
        return actions;
    }
    // Heuristic function
    // This case - Manhattan distance of each box to its closest goal
    public int h(BoxPoint[] boxes, List<Point> goals, BoxPoint changed, int oldH) {
        // If boxes did not change, do not update
        if (changed == null) return oldH;

        // greedy matching
        // If distance to best goal gets large, update matching
        // Point bestGoal = goals.get(changed.closestGoalId);
        //int newDist = Math.abs(bestGoal.x - changed.x) + Math.abs(bestGoal.y - changed.y);
        //for (BoxPoint b : boxes) if (b.dist < newDist) return greedyMatching(boxes, goals);

        // closest matching
        // update the closest matching
        int newDist = goals.stream().map(g -> Math.abs(changed.x - g.x) + Math.abs(changed.y - g.y))
                .reduce(0, Integer::min);

        int res = oldH - changed.dist;
        changed.dist = newDist;
        return res + changed.dist;
    }
    // State
    static class Node implements Comparable<Node>, Cloneable {
        BoxPoint[] boxes;
        BoardSlim board;
        Node parent;
        SAction pa;
        int g, h, hash;

        public Node(BoxPoint[] boxes, BoardSlim board, Node parent, SAction pa, int g, int h) {
            this.boxes = boxes;
            this.board = board;
            this.parent = parent;
            this.pa = pa;
            this.g = g;
            this.h = h;
            this.hash = -1;
        }

        public Node clone() {
            BoxPoint[] newBoxes = new BoxPoint[boxes.length];
            for (int i = 0; i < boxes.length; i++) {
                BoxPoint box = boxes[i];
                newBoxes[i] = new BoxPoint(box.x, box.y, box.dist, box.closestGoalId);
            }
            return new Node(newBoxes, board.clone(), parent, pa, g, h);
        }

        public BoxPoint moveBox(int x, int y, int tx, int ty) {
            board.moveBox((byte) x, (byte) y, (byte) tx, (byte) ty);
            BoxPoint box = null;
            for (BoxPoint b : boxes) if (b.x == x && b.y == y) {
                box = b;
                box.x = tx;
                box.y = ty;
                break;
            }
            Arrays.sort(boxes, (l, r) -> l.x == r.x ? r.y - l.y : r.x - l.x);
            hash = -1;
            return box;
        }

        public boolean equals(Object c) {
            if (!(c instanceof Node)) return false;
            if (c == this) return true;
            return this.hashCode() == c.hashCode();
        }

        public int hashCode() {
            if (hash != -1) return hash;
            return hash = (Arrays.stream(boxes).map(b -> b.x + "," + b.y + ",").reduce("", String::concat) +
                    board.playerX + "," + board.playerY).hashCode();
        }

        public int compareTo(Node o) {
            return Integer.compare(this.g + this.h, o.g + o.h);
        }

        public String toString() {
            return "<" + ((parent == null) ? "[null]" : parent.toString()) + " " +
                    ((pa == null) ? "[null]" : pa.toString()) + g + " " + h + ">";
        }
    }
    // Box point extension with two extra variables (id of closest goal, and distance to it)
    static class BoxPoint extends Point {
        int dist, closestGoalId;

        public BoxPoint(int x, int y, int dist, int closestGoalId) {
            super(x, y);
            this.dist = dist;
            this.closestGoalId = closestGoalId;
        }
    }
    // Point record
    static class Point implements Comparable<Point> {
        int x, y;
        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
        public int compareTo(Point o) {
            return Integer.compare(this.hashCode(), o.hashCode());
        }
    }
    // Class for finding dead squares
    static class DeadSquareDetector {
        public static boolean[][] detectSimple(BoardSlim board) {
            boolean[][] res = new boolean[board.width()][board.height()];
            // Flood fill for dead squares, starting from each goal
            for (Point goal : findGoals(board)) pull(board, res, goal.x, goal.y, new int[]{0, 1, 0, -1});
            for (int i = 0; i < board.width(); i++) for (int j = 0; j < board.height(); j++) res[i][j] ^= true;
            return res;
        }

        public static void pull(BoardSlim board, boolean[][] res, int x, int y, int[] dirs) {
            res[x][y] = true;
            for (int i = 0; i < 4; i++) {
                int nx = x + dirs[i], ny = y + dirs[(i + 1) % 4];
                if (nx < 1 || ny < 1 || nx > res.length - 2 || ny > res[0].length - 2) continue;
                if (res[nx][ny] || (board.tiles[nx][ny] & STile.WALL_FLAG) != 0 ||
                        (board.tiles[nx + dirs[i]][ny + dirs[(i + 1) % 4]] & STile.WALL_FLAG) != 0) continue;
                pull(board, res, nx, ny, dirs);
            }
        }
    }
    // Helper that matches each box to its closest goal, taking other matches into account
    // Returns heuristic (sum of Manhattan distances of matches)
    static int greedyMatching(BoxPoint[] boxes, List<Point> goals) {
        Queue<BoxPoint> pq = new PriorityQueue<>(Comparator.comparing(a -> a.dist));
        for (int i = 0; i < boxes.length; i++) for (int j = 0; j < goals.size(); j++) {
            BoxPoint b = boxes[i]; Point g = goals.get(j);
            pq.add(new BoxPoint(i, j, Math.abs(b.x - g.x) + Math.abs(b.y - g.y), -1));
        }
        int matchedBoxes = 0, matchedGoals = 0, res = 0;
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
    static List<Point> findGoals(BoardSlim board) {
        List<Point> res = new ArrayList<>();
        int id = 0;
        for (int i = 1; i < board.width() - 1; i++) for (int j = 1; j < board.height() - 1; j++)
            if ((STile.PLACE_FLAG & board.tiles[i][j]) != 0) res.add(new Point(i, j));
        return res;
    }
    // Helper for finding all boxes in a board
    static BoxPoint[] findBoxes(BoardSlim board) {
        List<BoxPoint> res = new ArrayList<>();
        for (int i = 1; i < board.width() - 1; i++) for (int j = 1; j < board.height() - 1; j++)
            if ((STile.BOX_FLAG & board.tiles[i][j]) != 0) res.add(new BoxPoint(i, j, -1, -1));
        res.sort((l, r) -> l.x == r.x ? r.y - l.y : r.x - l.x);
        return res.toArray(BoxPoint[]::new);
    }
}
