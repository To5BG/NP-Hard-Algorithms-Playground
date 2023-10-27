import solver.Bind;
import solver.CSolution;
import solver.Pair;
import solver.Problem;
import solver.Solver;
import solver.VariableBind;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BinaryStrings {
    public static void main(String[] args) {

        Solver<String> s = new Solver<String>()
                .addParameter("N", null)
                .addVariable("bitstr", new VariableBind(
                        List.of(), i -> List.of(0, 1),
                        List.of("N"), i -> new Integer[(Integer) i.get(0)]));

        Map<String, Object> model = new HashMap<>();
        model.put("N", 20);

        StringBuilder ss = new StringBuilder();
        CSolution<String> res = s.solve(model, Problem.ALL, m -> {
            ss.setLength(0);
            for (Integer i : m.get("bitstr")) ss.append(i);
            return ss.toString();
        }, null);

        System.out.println("=====SOLUTION=====");
        System.out.println(res.count);
        //System.out.println(res.solutions);
    }
}