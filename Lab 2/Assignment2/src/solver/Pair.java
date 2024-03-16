package solver;

// Pair DAO for the variable binds
public class Pair {
    String left;
    VariableBind right;

    public Pair(String left, VariableBind right) {
        this.left = left;
        this.right = right;
    }
}