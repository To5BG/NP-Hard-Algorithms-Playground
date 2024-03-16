package solver;

import java.util.Map;

@FunctionalInterface
public interface PentaFunction<E, R> {
    R apply(Map<String, Object> params, Integer varIdx, Integer elIdx, Integer decision, E s);
}