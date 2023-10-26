import static java.lang.System.out;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

import agents.ArtificialAgent;
import game.actions.EDirection;
import game.actions.compact.*;
import game.board.compact.BoardCompact;
import game.board.oop.EEntity;
import game.board.oop.EPlace;
import game.board.slim.BoardSlim;
import game.board.slim.STile;


/**
 * The simplest Tree-DFS agent.
 *
 * @author Jimmy
 */
public class MyAgent extends ArtificialAgent {
    protected BoardCompact board;
    protected int searchedNodes;

    protected List<Point> goals;

    @Override
    protected List<EDirection> think(BoardCompact board) {
        this.board = board;
        searchedNodes = 0;
        this.goals = findGoals(board);

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
        // Init
        Map<Node, Integer> dist = new HashMap<>();
        Queue<Node> q = new PriorityQueue<>();
        List<Point> boxes = findBoxes(board, goals);
        Node start = new Node(boxes.toArray(Point[]::new), board, null, null, 0,
                h(null, boxes.stream().map(b -> b.dist).reduce(0, Integer::sum)));
        dist.put(start, 0);
        q.add(start);

        boolean[][] deadSquares = DeadSquareDetector.detectSimple(this.board);
//        System.out.println("dead squares: \n");
//        for (int y = 0 ; y < board.height() ; ++y) {
//            for (int x = 0 ; x < board.width() ; ++x)
//                System.out.print(CTile.isWall(board.tile(x, y)) ? '#' : (deadSquares[x][y] ? 'X' : '_'));
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
            List<CAction> actions = new ArrayList<>(4);
            // Add possible moves
            for (CMove move : CMove.getActions())
                if (move.isPossible(curr.board) && (!(curr.pa instanceof CMove) ||
                        !move.getDirection().equals(curr.pa.getDirection().opposite()))) actions.add(move);
            // Add possible pushes
            for (CPush push : CPush.getActions()) if (push.isPossible(curr.board)) actions.add(push);
            // For each possible action
            for (CAction action : actions) {
                Node next = curr.clone();
                EDirection dir = action.getDirection();
                int nextX = next.board.playerX + dir.dX;
                int nextY = next.board.playerY + dir.dY;
                Point movedBox = null;
                // Move player and set player minDist to -1 to trigger recomputation
                if (action instanceof CMove) {
                    next.board.movePlayer(next.board.playerX, next.board.playerY, nextX, nextY);
                    // Move box, cache moved box for heuristic recomputation
                    // Player minDist remains 0 (next to box in order to push) -> no recomputation
                } else if (action instanceof CPush) {
                    movedBox = next.moveBox(nextX, nextY, nextX + dir.dX, nextY + dir.dY);
                    next.board.movePlayer(next.board.playerX, next.board.playerY, nextX, nextY);
                }
                int newCost = curr.g + 1;
                // Don't consider if it does not improve on previous distance or if it leads to an unsolvable position
                if (newCost + curr.h >= dist.getOrDefault(next, Integer.MAX_VALUE) || (action instanceof CPush &&
                        Arrays.stream(next.boxes).anyMatch(b -> deadSquares[b.x][b.y]))) continue;
                dist.put(next, newCost);
                next.parent = curr;
                next.pa = action;
                next.g = newCost;
                next.h = h(movedBox, curr.h);
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
        // Print path
//        System.out.println(actions.stream().map(o -> o.toString().substring(0, 1)).collect(Collectors.joining()));
        return actions;
    }

    // Heuristic function
    // This case - Manhattan distance of each box to its closest goal
    public int h(Point changed, int oldH) {
        if (changed == null) return oldH;
        // If box was moved, update boxes-to-goals minDist
        int res = oldH - changed.dist;
        changed.dist = goals.stream().map(goal -> Math.abs(changed.x - goal.x) +
                Math.abs(changed.y - goal.y)).reduce(Integer.MAX_VALUE, Math::min);
        return res + changed.dist;
    }

    static class Node implements Comparable<Node>, Cloneable {
        Point[] boxes;
        BoardCompact board;
        Node parent;
        CAction pa;
        int g, h, hash;

        public Node(Point[] boxes, BoardCompact board, Node parent, CAction pa, int g, int h) {
            this.boxes = boxes;
            this.board = board;
            this.parent = parent;
            this.pa = pa;
            this.g = g;
            this.h = h;
            this.hash = -1;
        }

        public Node clone() {
            Point[] newBoxes = new Point[boxes.length];
            for (int i = 0; i < boxes.length; i++) {
                Point box = boxes[i];
                newBoxes[i] = new Point(box.x, box.y, box.dist);
            }
            return new Node(newBoxes, board.clone(), parent, pa, g, h);
        }

        public Point moveBox(int x, int y, int tx, int ty) {
            board.moveBox(x, y, tx, ty);
            Point box = null;
            for (Point b : boxes) if (b.x == x && b.y == y) {
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

    static class Point implements Comparable<Point> {
        int x, y, dist;

        public Point(int x, int y, int dist) {
            this.x = x;
            this.y = y;
            this.dist = dist;
        }

        public int compareTo(Point o) {
            return Integer.compare(this.hashCode(), o.hashCode());
        }
    }

    static class DeadSquareDetector {
        public static boolean[][] detectSimple(BoardCompact board) {
            boolean[][] res = new boolean[board.width()][board.height()];
            // Flood fill for dead squares, starting from each goal
            for (Point goal : findGoals(board)) pull(board.makeBoardSlim(), res, goal.x, goal.y);
            for (int i = 0; i < board.width(); i++) for (int j = 0; j < board.height(); j++) res[i][j] ^= true;
            return res;
        }

        public static void pull(BoardSlim board, boolean[][] res, int x, int y) {
            res[x][y] = true;
            int[] dirs = {0, 1, 0, -1};
            for (int i = 0; i < 4; i++) {
                int nx = x + dirs[i], ny = y + dirs[(i + 1) % 4];
                if (nx < 1 || ny < 1 || nx > res.length - 2 || ny > res[0].length - 2) continue;
                if (res[nx][ny] || (board.tiles[nx][ny] & STile.WALL_FLAG) != 0 ||
                        (board.tiles[nx + dirs[i]][ny + dirs[(i + 1) % 4]] & STile.WALL_FLAG) != 0) continue;
                pull(board, res, nx, ny);
            }
        }
    }

    static List<Point> findGoals(BoardCompact board) {
        List<Point> res = new ArrayList<>();
        for (int i = 1; i < board.width() - 1; i++) for (int j = 1; j < board.height() - 1; j++)
            if ((EPlace.SOME_BOX_PLACE_FLAG & board.tiles[i][j]) != 0) res.add(new Point(i, j, 0));
        return res;
    }

    static List<Point> findBoxes(BoardCompact board, List<Point> goals) {
        List<Point> res = new ArrayList<>();
        for (int i = 1; i < board.width() - 1; i++) for (int j = 1; j < board.height() - 1; j++)
            if ((EEntity.SOME_BOX_FLAG & board.tiles[i][j]) != 0) res.add(new Point(i, j, -1));
        for (Point box : res) {
            box.dist = goals.stream().map(goal -> Math.abs(box.x - goal.x) + Math.abs(box.y - goal.y))
                    .reduce(Integer.MAX_VALUE, Math::min);
        }
        res.sort((l, r) -> l.x == r.x ? r.y - l.y : r.x - l.x);
        return res;
    }
}
