package solver;

// Generic Pair DAO
public class Pair<L, R> {
    L left;
    R right;

    public Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }
}