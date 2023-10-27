package solver;

import java.util.ArrayList;
import java.util.List;

public class Node {
    public Node parent;
    public String nextValDecision;
    public Integer idx;
    public List<Integer> domain;
    public List<Node> children;
    public Integer weight;

    public Node(Node parent) {
        this.parent = parent;
        this.children = new ArrayList<>();
    }
}
