package solver;

// Pair DAO - used to map decision to (var, element) pair
public class Pair {
    String var; // Variable name
    Integer eid; // Element index (index within this variable)

    public Pair(String var, Integer r) {
        this.var = var;
        this.eid = r;
    }
}