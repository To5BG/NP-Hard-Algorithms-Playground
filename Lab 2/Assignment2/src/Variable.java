import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

// Variable wrapper
public class Variable {
    // domain of possible values
    List<Integer> domain;
    // array of possible values
    Integer[] value;

    public Variable(List<Integer> domain, Integer[] value, int size) {
        this.domain = domain;
        this.value = value;
    }

    // all possible values, comma delimited
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

// binds string arguments to values based on f
class Bind {
    // bound arguments (as string names)
    List<String> args;

    // function to evaluate
    Function<List<Object>, ?> f;

    public Bind(List<String> args, Function<List<Object>, ?> f) {
        this.args = args;
        this.f = f;
    }

    public Object apply(Map<?, Object> mapper) {
        return this.f.apply(this.args.stream().map(mapper::get).collect(Collectors.toList()));
    }
}

// Constraint wrapper
class Constraint {
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

// Symmetry breaker wrapper
class SymmetryBreaker {

    // Weight of initial search (for instance, if model is built such that each solution counts as two,
    // then this value should be 2)
    Integer initialWeight;

    // Solver to which this symmetry breaker is bound to
    Solver<?> bind;

    // Function that checks if a symmetry can be broken
    BiFunction<Node, Integer, Boolean> checkSymmetry;

    // Weight function for each branch (how much does each symmetry cut the search space)
    BiFunction<Node, Integer, Integer> calculateCountWeight;

    public SymmetryBreaker(Solver<?> bind, BiFunction<Node, Integer, Boolean> checkSymmetry,
                           BiFunction<Node, Integer, Integer> calculateCountWeight, Integer initialWeight) {
        this.bind = bind;
        this.checkSymmetry = checkSymmetry;
        this.calculateCountWeight = calculateCountWeight;
        this.initialWeight = initialWeight;
    }
}

// Types of problems that can be solved
enum Problem {
    SATISFY,
    COUNT,
    ALL,
    MIN,
    MAX
}