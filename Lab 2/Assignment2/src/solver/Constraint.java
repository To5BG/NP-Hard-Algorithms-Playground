package solver;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

// Constraint wrapper
public class Constraint {
    List<String> parameterNames; // Parameter arguments
    List<String> variableNames; // Variable arguments
    BiFunction<List<Object>, List<Integer[]>, Boolean> constr; // (parameters, variable values) -> does it satisfy
    PentaFunction<Map<String, Object>, String, Integer, Integer, Map<String, Integer[][]>, Boolean> propFunc;
    // (parameters, variable, element index, decision, full domain) -> can you propagate

    public Constraint(List<String> parameterNames, List<String> variableNames,
                      BiFunction<List<Object>, List<Integer[]>, Boolean> constr, PentaFunction<
            Map<String, Object>, String, Integer, Integer, Map<String, Integer[][]>, Boolean> propFunc) {
        this.parameterNames = parameterNames;
        this.variableNames = variableNames;
        this.constr = constr;
        this.propFunc = propFunc;
    }

    public Constraint(String variable, PentaFunction<
            Map<String, Object>, String, Integer, Integer, Map<String, Integer[][]>, Boolean> pf) {
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