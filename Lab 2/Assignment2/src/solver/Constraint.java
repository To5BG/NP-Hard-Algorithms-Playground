package solver;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

// Constraint wrapper
public class Constraint {

    // Parameter arguments
    List<String> parameterNames;

    // Variable arguments
    List<String> variableNames;

    // Constraint function ((parameters, variable values) -> does it satisfy)
    BiFunction<List<Object>, List<Integer[]>, Boolean> constr;

    // Propagation function ((parameters, variable, element index, decision, full domain) -> can you propagate)
    PropagatorFunction propFunc;

    public Constraint(List<String> parameterNames, List<String> variableNames,
                      BiFunction<List<Object>, List<Integer[]>, Boolean> constr, PropagatorFunction propFunc) {
        this.parameterNames = parameterNames;
        this.variableNames = variableNames;
        this.constr = constr;
        this.propFunc = propFunc;
    }

    public Constraint(String variable, PropagatorFunction pf) {
        this(List.of(), List.of(variable), null, pf);
    }

    public Constraint(String variable, String builtinConstraint, int arg) {
        this(List.of(), List.of(variable), null, ConstraintPropagators.get(builtinConstraint, arg));
    }

    public boolean apply(Map<String, Object> par, Map<String, Integer[]> var) {
        if (this.constr == null) return true;
        return this.constr.apply(
                this.parameterNames.stream().map(par::get).collect(Collectors.toList()),
                this.variableNames.stream().map(var::get).collect(Collectors.toList()));
    }
}
