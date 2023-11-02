import solver.CSolution;
import solver.Constraint;
import solver.ConstraintPropagators;
import solver.Problem;
import solver.Solver;
import solver.VariableBind;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Combinations {
    public static void main(String[] args) {
        Solver<int[]> s = new Solver<int[]>()
                .addParameter("N", null)
                .addParameter("K", null)

                .addVariable("comb", new VariableBind(
                        List.of("N"), i -> IntStream.rangeClosed(1, (Integer) i.get(0))
                        .boxed().collect(Collectors.toList()),
                        List.of("K"), i -> new Integer[(Integer) i.get(0)]))

                // Remove alldiff to allow repetitions
                // Constraint to check if picked values are all different and in decreasing order
                .addConstraint(new Constraint(List.of(), List.of("comb"), (p, v) -> {
                    int prev = Integer.MAX_VALUE;
                    for (Integer i : v.get(0)) {
                        if (i >= prev) return false;
                        prev = i;
                    }
                    return true;
                }, ConstraintPropagators.get("alldiff", -1)));

        Map<String, Object> model = new HashMap<>();
        model.put("N", 1000);
        model.put("K", 2);

        CSolution<int[]> res = s.solve(model, Problem.ALL, m ->
                Arrays.stream(m.get("comb")).mapToInt(Integer::intValue).toArray(), null);

        System.out.println("=====SOLUTION=====");
        System.out.println(res.count);
        //for (int[] i : res.solutions)
        //    System.out.println(Arrays.stream(i).mapToObj(ii -> ii + ",").reduce("", String::concat));
    }
}