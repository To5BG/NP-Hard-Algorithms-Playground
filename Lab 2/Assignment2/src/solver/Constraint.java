package solver;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

// Constraint wrapper
public class Constraint {
    // Parameter arguments
    List<String> parargs;

    // Variable arguments
    List<String> varargs;

    // Constraint function
    BiFunction<List<Object>, List<Integer[]>, Boolean> constr;

    public Constraint(List<String> parargs, List<String> varargs,
                      BiFunction<List<Object>, List<Integer[]>, Boolean> constr) {
        this.parargs = parargs;
        this.varargs = varargs;
        this.constr = constr;
    }

    public boolean apply(Map<String, Object> par, Map<String, Integer[]> var) {
        return this.constr.apply(
                this.parargs.stream().map(par::get).collect(Collectors.toList()),
                this.varargs.stream().map(var::get).collect(Collectors.toList()));
    }
}
