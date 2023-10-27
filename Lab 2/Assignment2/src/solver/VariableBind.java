package solver;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// binds string arguments to values based on f
public class VariableBind extends Bind {
    // bound arguments (as string names)
    List<String> domainArgs;

    // function to evaluate
    Function<List<Object>, List<Integer>> domainF;

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

