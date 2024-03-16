package solver;

// Triple DAO - used to map decision to (var_name, var_idx, element) pair
public class Triple {
    String var; // Variable name
    Integer varIdx; // Variable index
    Integer eid; // Element index (index within this variable)

    public Triple(String var, Integer i, Integer ei) {
        this.var = var;
        varIdx = i;
        eid = ei;
    }
}