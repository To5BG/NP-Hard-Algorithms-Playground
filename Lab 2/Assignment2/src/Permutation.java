import solver.CSolution;
import solver.Problem;
import solver.Solver;
import solver.VariableBind;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Permutation {
    public static void main(String[] args) {
        Map<String, Object> model = new HashMap<>();
        model.put("N", 10);

        CSolution<int[]> res = new Solver<int[]>()
                .addParameter("N", null)
                .addVariable("comb", new VariableBind(
                        List.of("N"), i -> IntStream.rangeClosed(1, (Integer) i.get(0))
                        .boxed().collect(Collectors.toList()),
                        List.of("N"), i -> {
                    Integer[] v = new Integer[(Integer) i.get(0)];
                    Arrays.fill(v, Integer.MIN_VALUE);
                    return v;
                }))
                .addConstraint("comb", "alldiff", -1)
                .solve(model, Problem.ALL, m -> Arrays.stream(m.get("comb")).mapToInt(Integer::intValue).toArray(),
                        null);

        System.out.println("=====SOLUTION=====");
        System.out.println(res.count);
        //for (int[] i : res.solutions)
        //    System.out.println(Arrays.stream(i).mapToObj(ii -> ii + ",").reduce("", String::concat));
    }
}