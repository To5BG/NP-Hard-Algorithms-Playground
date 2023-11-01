import solver.CSolution;
import solver.Problem;
import solver.PropagatorFunction;
import solver.Solver;
import solver.SymmetryCheckFunction;
import solver.SymmetryWeightFunction;
import solver.VariableBind;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NQueens {
    public static void main(String[] args) {

        PropagatorFunction positiveDiagonalPropagation = new PropagatorFunction() {
            @Override
            public Boolean apply(Map<String, Object> params, String var, Integer idx, Integer decision,
                                 Map<String, Integer[][]> domains) {
                domains.computeIfPresent(var, (k, v) -> {
                    for (int i = 0; i < v.length; i++) {
                        if (idx == i) continue;
                        Integer[] ll = v[i];
                        int removed = idx + decision - i;
                        for (int j = 0; j < ll.length; j++)
                            if (ll[j].equals(removed)) ll[j] = Integer.MIN_VALUE;
                    }
                    return v;
                });
                return false;
            }
        };

        PropagatorFunction negativeDiagonalPropagation = new PropagatorFunction() {
            @Override
            public Boolean apply(Map<String, Object> params, String var, Integer idx, Integer decision,
                                 Map<String, Integer[][]> domains) {
                domains.computeIfPresent(var, (k, v) -> {
                    for (int i = 0; i < v.length; i++) {
                        if (idx == i) continue;
                        Integer[] ll = v[i];
                        int removed = decision - idx + i;
                        for (int j = 0; j < ll.length; j++)
                            if (ll[j].equals(removed)) ll[j] = Integer.MIN_VALUE;
                    }
                    return v;
                });
                return false;
            }
        };

        Solver<Object> s = new Solver<>()
                .addParameter("N", null)
                // Model board as an n-array, where q[i] = column of queen on ith row
                // Ensures only one queen per row
                .addVariable("q", new VariableBind(List.of("N"), i -> IntStream.range(0, (Integer) i.get(0))
                        .boxed().collect(Collectors.toList()), List.of("N"), i -> new Integer[(Integer) i.get(0)]))
                // No queens on same column
                .addConstraint("q", "alldiff", -1)
                // Constraints for diagonals
                // Check that for no two queens q[i] + i == q[j] + j (or / diagonal)
                .addConstraint("q", positiveDiagonalPropagation)
                // Check that for no two queens q[i] - i == q[j] - j (or \ diagonal)
                .addConstraint("q", negativeDiagonalPropagation);

        // Symmetry breaker, arguments explained below, small comments on their location among the solver
        s.addSymmetryBreaker(
                // Checker function -> when branches can be skipped due to symmetry
                // For this problem, simplest symmetry breaker is enforcing first row queen to be
                // only on the first half -> Breaks x-axis mirrored solutions
                new SymmetryCheckFunction() {
                    @Override
                    public Boolean apply(Map<String, Object> params, String var, Integer idx, Integer decision,
                                         Integer[] domain) {
                        return idx == 0 && decision > ((Integer) params.get("N") - 1) / 2;
                    }
                },
                // Weight function -> determine how much should we count each individual symmetrical branch
                // For this problem, it is always 2, except if
                // we are on the middle column of a board with odd dimensions
                new SymmetryWeightFunction() {
                    @Override
                    public Integer apply(Map<String, Object> params, String var, Integer idx, Integer decision,
                                         Integer weight) {
                        int n = (Integer) params.get("N");
                        return Math.min(weight, (idx == 0 && (n % 2 != 0) &&
                                (decision == (n - 1) / 2)) ? 1 : 2);
                    }
                }, 2);

        Map<String, Object> model = new HashMap<>();
        model.put("N", 12);

        CSolution<Object> res = s.solve(model, Problem.COUNT, m -> m, null);

        System.out.println("=====SOLUTION=====");
        System.out.println(res.count);
    }
}