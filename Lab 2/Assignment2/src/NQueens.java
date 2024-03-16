import solver.CSolution;
import solver.Problem;
import solver.Solver;
import solver.VariableBind;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NQueens {
    public static void main(String[] args) {
        Map<String, Object> model = new HashMap<>();
        model.put("N", 13);

        CSolution<Object> res = new Solver<>()
                .addParameter("N", null)
                // Model board as an n-array, where q[i] = column of queen on ith row -> only one queen per row
                .addVariable("q", new VariableBind(List.of("N"), i -> IntStream.range(0, (Integer) i.get(0))
                        .boxed().collect(Collectors.toList()), List.of("N"), i -> {
                    Integer[] v = new Integer[(Integer) i.get(0)];
                    Arrays.fill(v, Integer.MIN_VALUE);
                    return v;
                }))
                // No queens on same column
                .addConstraint("q", "alldiff", -1)
                // Constraints for diagonals
                // Check that for no two queens q[i] + i == q[j] + j (or positive (/) diagonal)
                .addConstraint("q", (params, var, idx, decision, domains) -> {
                    BitSet[] v = domains[var];
                    for (int i = 0; i < v.length; i++) {
                        if (v[i] == null) continue;
                        int removed = idx + decision - i;
                        if (removed < 0) break;
                        v[i].clear(removed);
                        if (v[i].isEmpty()) return true;
                    }
                    return false;
                })
                // Check that for no two queens q[i] - i == q[j] - j (or negative (\) diagonal)
                .addConstraint("q", (params, var, idx, decision, domains) -> {
                    BitSet[] v = domains[var];
                    for (int i = 0; i < v.length; i++) {
                        if (v[i] == null) continue;
                        int removed = decision - idx + i;
                        if (removed < 0) continue;
                        v[i].clear(removed);
                        if (v[i].isEmpty()) return true;
                    }
                    return false;
                })
                .addSymmetryBreaker(
                        // Checker function -> when branches can be skipped due to symmetry
                        // For this problem, simplest symmetry breaker is enforcing first row queen to be
                        // only on the first half -> Breaks x-axis mirrored solutions
                        (params, var, idx, decision, domain) ->
                                idx == 0 && decision > ((Integer) params.get("N") - 1) / 2,
                        // Weight function -> determine how much should we count each individual symmetrical branch
                        // For this problem, it is always 2, except if on the middle column of an odd dimensional board
                        (params, var, idx, decision, weight) -> {
                            int n = (Integer) params.get("N");
                            return Math.min(weight, (idx == 0 && (n % 2 != 0) &&
                                    (decision == (n - 1) / 2)) ? 1 : 2);
                        }, 2)
                .solve(model, Problem.COUNT, m -> m, null);

        System.out.println("=====SOLUTION=====");
        System.out.println(res.count);
    }
}