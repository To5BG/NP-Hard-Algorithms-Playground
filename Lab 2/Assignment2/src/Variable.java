import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Variable {
    List<Integer> domain;
    Integer[] value;

    public Variable(List<Integer> domain, Integer[] value, int size) {
        this.domain = domain;
        this.value = value;
    }

    public String toString() {
        if (value.length == 1) return "" + value[0];
        StringBuilder r = new StringBuilder("[");
        for (Integer v : value) {
            r.append(v);
            r.append(",");
        }
        r.setCharAt(r.length() - 1, ']');
        return r.toString();
    }
}

class Bind {
    List<String> args;
    Function<List<Object>, ?> f;

    public Bind(List<String> args, Function<List<Object>, ?> f) {
        this.args = args;
        this.f = f;
    }

    public Object apply(Map<?, Object> mapper) {
        return this.f.apply(this.args.stream().map(mapper::get).collect(Collectors.toList()));
    }
}

class Constraint {
    List<String> parargs;
    List<String> varargs;
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

class SymmetryBreaker {
    Integer initialWeight;
    Solver<?> bind;
    BiFunction<Node, Integer, Boolean> checkSymmetry;
    BiFunction<Node, Integer, Integer> calculateCountWeight;

    public SymmetryBreaker(Solver<?> bind, BiFunction<Node, Integer, Boolean> checkSymmetry,
                           BiFunction<Node, Integer, Integer> calculateCountWeight, Integer initialWeight) {
        this.bind = bind;
        this.checkSymmetry = checkSymmetry;
        this.calculateCountWeight = calculateCountWeight;
        this.initialWeight = initialWeight;
    }
}

enum Problem {
    SATISFY,
    COUNT,
    ALL,
    MIN,
    MAX
}