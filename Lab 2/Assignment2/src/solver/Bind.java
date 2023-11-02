package solver;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// Binds string arguments to values based on f
public class Bind {
    List<String> args; // Bound arguments (as string names)
    Function<List<Object>, ?> f; // Function to evaluate value

    public Bind(List<String> args, Function<List<Object>, ?> f) {
        this.args = args;
        this.f = f;
    }

    public Object apply(Map<?, Object> mapper) {
        return this.f.apply(this.args.stream().map(mapper::get).collect(Collectors.toList()));
    }
}