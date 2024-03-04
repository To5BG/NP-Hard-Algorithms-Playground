import solver.CSolution;
import solver.Problem;
import solver.Solver;
import solver.VariableBind;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Subset {
    public static void main(String[] args) {
        Map<String, Object> model = new HashMap<>();
        model.put("N", 20);

        CSolution<int[]> res = new Solver<int[]>()
                .addParameter("N", null)
                .addVariable("comb", new VariableBind(List.of(), i -> new ArrayList<>(List.of(0, 1)),
                        List.of("N"), i -> {
                    Integer[] v = new Integer[(Integer) i.get(0)];
                    Arrays.fill(v, Integer.MIN_VALUE);
                    return v;
                }))
                .solve(model, Problem.ALL, m -> {
                    List<Integer> r = new ArrayList<>();
                    Integer[] mr = m.get("comb");
                    for (int i = 0; i < mr.length; i++) if (mr[i] != 0) r.add(i + 1);
                    int[] rr = new int[r.size()];
                    for (int i = 0; i < r.size(); i++) rr[i] = r.get(i);
                    return rr;
                }, null);

        System.out.println("=====SOLUTION=====");
        System.out.println(res.count);
        //for (int[] i : res.solutions)
        //    System.out.println(Arrays.stream(i).mapToObj(ii -> ii + ",").reduce("", String::concat));
    }
}