package solver;

import java.util.ArrayList;
import java.util.List;

// Wrapper node that represents a decision along the tree
public class Node {

    public Node parent;
    public String nextVarDecision;
    public Integer elementIndex;
    public List<Integer> domain;
    public List<Node> children;
    public Integer weight;

    public Node(Node parent) {
        this.parent = parent;
        this.children = new ArrayList<>();
    }
}
