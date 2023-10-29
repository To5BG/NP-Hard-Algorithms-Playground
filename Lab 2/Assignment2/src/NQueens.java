import solver.CSolution;
import solver.Problem;
import solver.PropagatorFunction;
import solver.Solver;
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
                                 Map<String, List<List<Integer>>> domains) {
                domains.computeIfPresent(var, (k, v) -> {
                    for (int i = 0; i < v.size(); i++) {
                        if (idx == i) continue;
                        List<Integer> ll = v.get(i);
                        ll.remove((Integer)(idx + decision - i));
                    }
                    return v;
                });
                return false;
            }
        };

        PropagatorFunction negativeDiagonalPropagation = new PropagatorFunction() {
            @Override
            public Boolean apply(Map<String, Object> params, String var, Integer idx, Integer decision,
                                 Map<String, List<List<Integer>>> domains) {
                domains.computeIfPresent(var, (k, v) -> {
                    for (int i = 0; i < v.size(); i++) {
                        if (idx == i) continue;
                        List<Integer> ll = v.get(i);
                        ll.remove((Integer) (decision - idx + i));
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
                (nn, j) -> nn.elementIndex == 0 && nn.domain.get(j) > ((Integer) s.parameters.get("N") - 1) / 2,
                // Weight function -> determine how much should we count each individual symmetrical branch
                // For this problem, it is always 2, except if
                // we are on the middle column of a board with odd dimensions
                (nn, j) -> Math.min(nn.weight, (nn.elementIndex == 0 && ((Integer) s.parameters.get("N") % 2 != 0) &&
                        (nn.domain.get(j) == ((Integer) s.parameters.get("N") - 1) / 2)) ? 1 : 2),
                2);

        Map<String, Object> model = new HashMap<>();
        model.put("N", 10);

        CSolution<Object> res = s.solve(model, Problem.COUNT, m -> m, null);

        System.out.println("=====SOLUTION=====");
        System.out.println(res.count);
    }
}