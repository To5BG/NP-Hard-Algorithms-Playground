package solver;

import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

// Constraint wrapper
public class Constraint {
    List<String> parameterNames; // Parameter arguments
    List<String> variableNames; // Variable arguments
    BiFunction<List<Object>, List<Integer[]>, Boolean> constr; // (parameters, variable values) -> does it satisfy
    PentaFunction<BitSet[][], Boolean> propFunc;
    // (parameters, var_idx, element_idx, decision, full domain) -> can you propagate

    public Constraint(List<String> parameterNames, List<String> variableNames,
                      BiFunction<List<Object>, List<Integer[]>, Boolean> constr,
                      PentaFunction<BitSet[][], Boolean> propFunc) {
        this.parameterNames = parameterNames;
        this.variableNames = variableNames;
        this.constr = constr;
        this.propFunc = propFunc;
    }

    public Constraint(String variable, PentaFunction<BitSet[][], Boolean> pf) {
        this(List.of(), List.of(variable), null, pf);
    }

    public Constraint(String variable, String builtinConstraint, int arg) {
        this(List.of(), List.of(variable), null, ConstraintPropagators.get(builtinConstraint, arg));
    }

    public boolean check(Map<String, Object> par, Map<String, Integer[]> var) {
        if (this.constr == null) return true;
        return this.constr.apply(
                this.parameterNames.stream().map(par::get).collect(Collectors.toList()),
                this.variableNames.stream().map(var::get).collect(Collectors.toList()));
    }
}