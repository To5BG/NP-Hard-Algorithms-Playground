import static java.lang.System.out;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.stream.Collectors;

import agents.ArtificialAgent;
import game.actions.EDirection;
import game.actions.compact.*;
import game.board.compact.BoardCompact;
import game.board.oop.ESpace;
import game.board.oop.EEntity;
import game.board.oop.EPlace;


/**
 * The simplest Tree-DFS agent.
 *
 * @author Jimmy
 */
public class MyAgent extends ArtificialAgent {
    protected BoardCompact board;
    protected int searchedNodes;

    @Override
    protected List<EDirection> think(BoardCompact board) {
        this.board = board;
        searchedNodes = 0;
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
        Map<BoardCompact, Double> dist = new HashMap<>();
        Queue<Node> q = new PriorityQueue<>();
        Config conf = Config.buildConfig(this.board);

        dist.put(this.board, 0.0);
        q.add(new Node(conf, null, null, 0.0, h(conf, null,
                conf.boxes.stream().map(b -> (double) b.dist).reduce(0.0, Double::sum))));
        Node curr = null;

        boolean[][] deadSquares = DeadSquareDetector.detect(this.board);
        while (!q.isEmpty()) {
            curr = q.poll();
            searchedNodes++;
            // Guard clauses
            if (curr.s.board.isVictory()) break;
            if (curr.g > maxCost) continue;
            List<CAction> actions = new ArrayList<>(4);
            // Add possible moves and pushes
            for (CMove move : CMove.getActions())
                if (move.isPossible(curr.s.board) &&
                        !move.getDirection().equals(move.getDirection().opposite())) actions.add(move);
            for (CPush push : CPush.getActions()) if (push.isPossible(curr.s.board)) actions.add(push);
            for (CAction action : actions) {
                Config next = curr.s.clone();
                EDirection dir = action.getDirection();
                int steps = action.getSteps();
                Point movedBox = null;
                // Move player and set player minDist to -1 to trigger recomputation
                if (action instanceof CMove) {
                    next.board.movePlayer(next.board.playerX, next.board.playerY,
                            (byte) (next.board.playerX + dir.dX * steps), (byte) (next.board.playerY + dir.dY * steps));
                    next.playerDist = -1;
                    // Move box, cache moved box for heuristic recomputation
                    // Player minDist remains 0 (next to box in order to push) -> no recomputation
                } else if (action instanceof CPush) {
                    movedBox = next.moveBox((byte) (next.board.playerX + dir.dX), (byte) (next.board.playerY + dir.dY),
                            (byte) (next.board.playerX + dir.dX * steps + dir.dX),
                            (byte) (next.board.playerY + dir.dY * steps + dir.dY));
                    next.board.movePlayer(next.board.playerX, next.board.playerY,
                            (byte) (next.board.playerX + dir.dX * steps), (byte) (next.board.playerY + dir.dY * steps));
                }
                double newCost = curr.g + action.getSteps();
                // Don't consider if it does not improve on previous distance or if it leads to an unsolvable position
                if (newCost + curr.h >= dist.getOrDefault(next.board, Double.MAX_VALUE) || (action instanceof CPush &&
                        next.boxes.stream().anyMatch(b -> deadSquares[b.x][b.y]))) continue;
                dist.put(next.board, newCost);
                Node newState = new Node(next, curr, action, newCost, h(next, movedBox, curr.h));
                q.add(newState);
            }
        }
        // Backtracking to build action chain
        if (curr == null) return null;
        List<EDirection> actions = new LinkedList<>();
        while (!curr.s.board.equals(this.board)) {
            actions.add(0, curr.pa.getDirection());
            curr = curr.parent;
        }
        // Print path
        // System.out.println(actions.stream().map(o -> o.toString().substring(0, 1)).collect(Collectors.joining()));
        return actions;
    }

    public static double h(Config b, Point changed, double oldH) {
        if (changed == null && b.playerDist != -1) return oldH;
        // If box was moved, update boxes-to-goals minDist
        if (changed != null) {
            double res = oldH - changed.dist;
            changed.dist = (byte) (b.goals.stream().map(goal -> Math.abs(changed.x - goal.x) +
                    Math.abs(changed.y - goal.y)).reduce(Integer.MAX_VALUE, Math::min).intValue());
            return res + changed.dist;
        }
        // If player moved, update player-to-box minDist
        double res = oldH - b.playerDist;
        b.playerDist = (byte) (b.boxes.stream().map(box ->
                        Math.abs(box.x - b.board.playerX) + Math.abs(box.y - b.board.playerY))
                .reduce(0, Integer::min) - 1);
        return res + b.playerDist;
    }


    static class Node implements Comparable<Node> {
        Config s;
        Node parent;
        CAction pa;
        Double g;
        Double h;

        public Node(Config s, Node parent, CAction pa, Double g, Double h) {
            this.s = s;
            this.parent = parent;
            this.pa = pa;
            this.g = g;
            this.h = h;
        }

        @Override
        public int compareTo(Node o) {
            return Double.compare(this.g + this.h, o.g + o.h);
        }

        public String toString() {
            return "<" + s.toString() + " " + ((pa == null) ? "[null]" : pa.toString()) + g + " " + h + ">";
        }
    }

    static class Point implements Comparable<Point> {
        byte x, y, dist;

        public Point(byte x, byte y, byte dist) {
            this.x = x;
            this.y = y;
            this.dist = dist;
        }

        public String toString() {
            return "(" + x + " " + y + ")";
        }

        @Override
        public int compareTo(Point o) {
            return Integer.compare(this.hashCode(), o.hashCode());
        }
    }

    static class Config implements Cloneable {
        List<Point> boxes;
        List<Point> goals;
        BoardCompact board;
        byte playerDist;

        public Config(List<Point> boxes, List<Point> goals, BoardCompact board, byte playerDist) {
            this.boxes = boxes;
            this.goals = goals;
            this.board = board;
            this.playerDist = playerDist;
        }

        public static Config buildConfig(BoardCompact b) {
            List<Point> boxes = new ArrayList<>();
            List<Point> goals = new ArrayList<>();
            for (byte i = 1; i < b.width() - 1; i++) {
                for (byte j = 1; j < b.height() - 1; j++) {
                    if ((EEntity.SOME_BOX_FLAG & b.tiles[i][j]) != 0) boxes.add(new Point(i, j, (byte) -1));
                    if ((EPlace.SOME_BOX_PLACE_FLAG & b.tiles[i][j]) != 0) goals.add(new Point(i, j, (byte) 0));
                }
            }
            for (Point box : boxes) {
                box.dist = (byte) (goals.stream().map(goal -> Math.abs(box.x - goal.x) + Math.abs(box.y - goal.y))
                        .reduce(Integer.MAX_VALUE, Math::min).intValue());
            }
            byte pdist = (byte) (boxes.stream().map(box -> Math.abs(box.x - b.playerX) + Math.abs(box.y - b.playerY))
                    .reduce(Integer.MAX_VALUE, Math::min) - 1);
            return new Config(boxes, goals, b, pdist);
        }

        public Config clone() {
            List<Point> newBoxes = new ArrayList<>(boxes.size());
            for (Point box : boxes) newBoxes.add(new Point(box.x, box.y, box.dist));
            List<Point> newGoals = new ArrayList<>(goals.size());
            for (Point goal : goals) newGoals.add(new Point(goal.x, goal.y, goal.dist));
            return new Config(newBoxes, newGoals, board.clone(), playerDist);
        }

        public Point moveBox(byte x, byte y, byte tx, byte ty) {
            board.moveBox(x, y, tx, ty);
            Point box = boxes.stream().filter(b -> b.x == x && b.y == y).findFirst().orElseThrow();
            box.x = tx;
            box.y = ty;
            return box;
        }

        public String toString() {
            return boxes.toString() + " " + goals.toString() + " " + board.toString();
        }
    }

    static class DeadSquareDetector {
        public static boolean[][] detect(BoardCompact board) {
            Config conf = Config.buildConfig(board);
            boolean[][] res = new boolean[board.width()][board.height()];
            // Flood fill for dead squares, starting from each goal
            for (Point goal : conf.goals) floodFill(board, res, goal.x, goal.y);
            // Flip boolean matrix (non-visited cannot be reached)
            for (int i = 1; i < board.width() - 1; i++) for (int j = 1; j < board.height() - 1; j++) res[i][j] ^= true;
            // Fill in entries to dead zones (2 dead squares and a wall)
            for (int i = 1; i < board.width() - 1; i++)
                for (int j = 1; j < board.height() - 1; j++)
                    if (!res[i][j] && !ESpace.WALL.isSpace(board.tiles[i][j]) &&
                            (EPlace.SOME_BOX_PLACE_FLAG & board.tiles[i][j]) == 0) {
                        byte[] dirs = new byte[]{1, 0, -1, 0};
                        // 4 bit flag for dead squares and walls on each cardinal directions
                        byte dead = 0, walls = 0;
                        for (int k = 0; k < 4; k++) {
                            walls |= (ESpace.WALL.isSpace(board.tiles[i + dirs[k]][j + dirs[(k + 1) % 4]])
                                    ? 1 : 0) << k;
                            dead |= ((res[i + dirs[k]][j + dirs[(k + 1) % 4]]) ? 1 : 0) << k;
                        }
                        // 2 dead squares in opposite directions and a wall perpendicular on them =>
                        // _#_    or     x_x      or  transpose of these two
                        // x_x           _#_
                        if ((((10 & dead) == 10) && ((5 & walls) != 0)) ||
                                (((5 & dead) == 5) && ((10 & walls) != 0))) {
                            res[i][j] = true;
                            break;
                        }
                    }
            return res;
        }

        public static void floodFill(BoardCompact board, boolean[][] res, byte i, byte j) {
            Queue<Point> q = new PriorityQueue<>();
            byte[] dirs = new byte[]{1, 0, -1, 0};
            q.add(new Point(i, j, (byte) 0));
            while (!q.isEmpty()) {
                Point curr = q.poll();
                byte walls = 0;
                for (byte k = 0; k < 4; k++) walls |=
                        (ESpace.WALL.isSpace(board.tiles[curr.x + dirs[k]][curr.y + dirs[(k + 1) % 4]]) ? 1 : 0) << k;
                if (curr.x != i && curr.y != j &&
                        ((3 & walls) == 3 || (6 & walls) == 6 || (9 & walls) == 9 || (12 & walls) == 12)) continue;
                res[curr.x][curr.y] = true;
                for (byte k = 0; k < 4; k++) {
                    int tx = curr.x + dirs[k];
                    int ty = curr.y + dirs[(k + 1) % 4];
                    boolean blocked = ((walls & (1 << k)) != 0) ||
                            ESpace.WALL.isSpace(board.tiles[2 * tx - curr.x][2 * ty - curr.y]);
                    if (!blocked && !res[tx][ty]) q.add(new Point((byte) tx, (byte) ty, (byte) 0));
                }
            }
        }
    }
}
