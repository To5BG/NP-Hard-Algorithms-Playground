package solver;

@FunctionalInterface
public interface PentaFunction<T, U, V, W, S, R> {
    R propagate(T t, U u, V v, W w, S s);
}