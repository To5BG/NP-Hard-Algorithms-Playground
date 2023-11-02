package solver;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// Binds string arguments to values based on f
public class VariableBind extends Bind {
    List<String> domainArgs; // Bound arguments (as string names)
    Function<List<Object>, List<Integer>> domainF; // Function to evaluate domain bind

    public VariableBind(List<String> domainArgs, Function<List<Object>, List<Integer>> domainF,
                        List<String> args, Function<List<Object>, ?> f) {
        super(args, f);
        this.domainArgs = domainArgs;
        this.domainF = domainF;
    }

    public List<Integer> applyDomain(Map<?, Object> mapper) {
        return this.domainF.apply(this.domainArgs.stream().map(mapper::get).collect(Collectors.toList()));
    }
}