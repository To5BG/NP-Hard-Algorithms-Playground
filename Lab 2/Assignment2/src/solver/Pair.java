package solver;

// Pair DAO - used to map decision to (var, element) pair
public class Pair {

    // Variable name
    String var;

    // Element index (index within this variable)
    Integer eid;

    public Pair(String var, Integer r) {
        this.var = var;
        this.eid = r;
    }
}