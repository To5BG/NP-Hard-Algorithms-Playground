package solver;

import java.util.Objects;
import java.util.function.Function;

@FunctionalInterface
public interface PentaFunction<T, U, V, W, S, R> {

    R apply(T t, U u, V v, W w, S s);

    @SuppressWarnings("unused")
    default <K> PentaFunction<T, U, V, W, S, K> andThen(Function<? super R, ? extends K> after) {
        Objects.requireNonNull(after);
        return (T t, U u, V v, W w, S s) -> after.apply(apply(t, u, v, w, s));
    }
}