import solver.Bind;
import solver.CSolution;
import solver.Pair;
import solver.Problem;
import solver.Solver;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Permutation {
    public static void main(String[] args) {
        Solver<int[]> s = new Solver<int[]>()
                .addParameter("N", null)

                .addVariable("comb", new Pair(
                        new Bind(List.of("N"), i -> IntStream.rangeClosed(1, (Integer) i.get(0))
                                .boxed().collect(Collectors.toList())),
                        new Bind(List.of("N"), i -> new Integer[(Integer) i.get(0)])))

                .addGlobalConstraint("comb", "alldiff", -1);

        Map<String, Object> model = new HashMap<>();
        model.put("N", 5);

        CSolution<int[]> res = s.solve(model, Problem.ALL, m ->
                Arrays.stream(m.get("comb")).mapToInt(Integer::intValue).toArray(), null);

        System.out.println("=====SOLUTION=====");
        System.out.println(res.count);
        for (int[] i : res.solutions)
            System.out.println(Arrays.stream(i).mapToObj(ii -> ii + ",").reduce("", String::concat));
    }
}