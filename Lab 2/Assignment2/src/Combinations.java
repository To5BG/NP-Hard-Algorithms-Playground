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

public class Combinations {
    public static void main(String[] args) {
        Solver<int[]> s = new Solver<int[]>()
                .addParameter("N", null)
                .addParameter("K", null)

                .addVariable("comb", new VariableBind(
                        List.of("N"), i -> IntStream.rangeClosed(1, (Integer) i.get(0))
                        .boxed().collect(Collectors.toList()),
                        List.of("K"), i -> new Integer[(Integer) i.get(0)]))

                .addGlobalConstraint("comb", "decrease", -1)
                .addGlobalConstraint("comb", "alldiff", -1);

        Map<String, Object> model = new HashMap<>();
        model.put("N", 2);
        model.put("K", 10);

        CSolution<int[]> res = s.solve(model, Problem.ALL, m ->
                Arrays.stream(m.get("comb")).mapToInt(Integer::intValue).toArray(), null);

        System.out.println("=====SOLUTION=====");
        System.out.println(res.count);
        for (int[] i : res.solutions)
            System.out.println(Arrays.stream(i).mapToObj(ii -> ii + ",").reduce("", String::concat));
    }
}