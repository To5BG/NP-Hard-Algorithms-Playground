import solver.CSolution;
import solver.Constraint;
import solver.Problem;
import solver.Solver;
import solver.VariableBind;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Sudoku {
    public static void main(String[] args) {

        int[][] grid = new int[][]{
                new int[]{6, 15, 1, 7, 3, -1, 2, 4, 11, 5, 10, 8, 13, 9, 16, 12},
                new int[]{2, 11, 10, -1, 13, 8, 5, 12, 9, 14, 1, 16, 3, 15, 6, -1},
                new int[]{9, 16, 14, 3, 7, 11, -1, 1, 2, 15, 12, 13, -1, 4, 8, 10},
                new int[]{-1, 8, -1, 13, -1, 16, 10, 15, 3, 6, 4, 7, 2, 14, -1, 1},
                new int[]{11, 9, -1, 16, 4, -1, -1, 13, 6, 7, -1, 15, 1, 2, 3, 8},
                new int[]{3, 1, 4, 12, 2, 6, 15, 11, 16, 8, 13, 10, 7, 5, 14, 9},
                new int[]{10, 2, 7, 8, 1, 3, 16, 14, 12, 9, -1, -1, 4, 13, 15, 6},
                new int[]{15, 6, 13, -1, 8, 5, -1, 7, 4, 3, 2, -1, 11, 12, 10, 16},
                new int[]{16, 14, 2, 15, 12, 13, -1, 5, 1, -1, 6, 9, 8, 3, 4, 11},
                new int[]{7, -1, 6, 10, 14, 2, 3, 16, 15, 11, 8, 4, 12, 1, 9, 5},
                new int[]{-1, 4, 9, 11, 10, 15, -1, 6, -1, 12, 7, 3, 14, 16, 13, 2},
                new int[]{-1, 12, 3, 5, -1, 4, 8, 9, 13, 2, 16, 14, -1, -1, 7, -1},
                new int[]{13, -1, 11, -1, 5, 9, 14, -1, 7, 1, 15, 6, -1, -1, 12, -1},
                new int[]{12, 3, 16, 1, 15, 7, -1, -1, 10, 13, -1, 5, -1, 11, 2, 14},
                new int[]{4, 7, 8, 9, 6, 1, 11, 2, 14, 16, 3, 12, 15, 10, 5, 13},
                new int[]{14, 5, 15, 6, 16, 12, 13, 10, 8, 4, 11, 2, 9, -1, 1, 3},
        };

        List<Integer> idx = IntStream.range(0, grid.length).boxed().sorted((m, n) -> {
            long a = Arrays.stream(grid[m]).filter(i -> i == -1).count();
            long b = Arrays.stream(grid[n]).filter(i -> i == -1).count();
            if (a == 0) a += grid.length;
            if (b == 0) b += grid.length;
            return Long.compare(a, b);
        }).flatMap(m -> IntStream.range(0, grid[m].length)
                .filter(i -> grid[m][i] == -1).map(i -> m * grid[m].length + i).boxed()).collect(Collectors.toList());
        for (int id : idx)
            System.out.print(id + " ");
        System.out.println();

        List<String> fullVar = IntStream.range(0, grid.length).mapToObj(i -> "puzzle_" + i)
                .collect(Collectors.toList());

        Solver<int[][]> solver = new Solver<int[][]>()
                .addParameter("N", null);

        for (int i = 0; i < grid.length; i++) {
            int finalI = i;
            solver = solver.addVariable("puzzle_" + i, new VariableBind(
                            List.of("N"), j -> IntStream.rangeClosed(1, (Integer) j.get(0))
                            .boxed().collect(Collectors.toList()),
                            List.of("N"), j -> new Integer[(Integer) j.get(0)]))

                    .addGlobalConstraint("puzzle_" + i, (Function<List<Object>, List<List<Integer>>>)
                            l -> {
                                List<List<Integer>> v = (List<List<Integer>>) l.get(1);
                                for (int ii = 0; ii < v.size(); ii++) {
                                    if (((AtomicBoolean) l.get(4)).get()) break;
                                    if (ii == (Integer) l.get(2)) continue;
                                    if (grid[finalI][ii] != -1) {
                                        List<Integer> newDomain = new ArrayList<>();
                                        newDomain.add(grid[finalI][ii]);
                                        v.set(ii, newDomain);
                                    }
                                }
                                return v;
                            }, -1)

                    .addGlobalConstraint("puzzle_" + i, "alldiff", -1)
                    .addGlobalConstraint("puzzle_" + i, "hi", -1)
                    .addGlobalConstraint("puzzle_" + i, "hi2", -1);
        }

        solver = solver
                .addConstraint(new Constraint(new ArrayList<>(), fullVar, (p, v) -> {
                    for (int i = 0; i < v.size(); i++)
                        for (int j = 0; j < v.size(); j++)
                            for (int k = j + 1; k < v.size(); k++)
                                if (v.get(j)[i].equals(v.get(k)[i])) return false;
                    return true;
                }))
                .addConstraint(new Constraint(new ArrayList<>(), fullVar, (p, v) -> {
                    int n = (int) Math.sqrt(grid.length);
                    for (int i = 0; i < n; i++)
                        for (int j = 0; j < n; j++) {
                            int finalJ = j;
                            List<Integer> section = IntStream.range(i * n, (i + 1) * n).flatMap(i1 ->
                                            Arrays.stream(grid[i1], finalJ * n, (finalJ + 1) * n))
                                    .filter(nn -> nn != -1).boxed().collect(Collectors.toList());
                            if (section.stream().distinct().count() != section.size()) return false;
                        }
                    return true;
                })).setVariableSelectionOrder(idx);

        Map<String, Object> model = new HashMap<>();
        model.put("N", grid.length);

        CSolution<int[][]> res = solver.solve(model, Problem.ALL, m ->
                IntStream.range(0, grid.length).mapToObj(i ->
                        Arrays.stream(Arrays.copyOf(m.get("puzzle_" + i), grid.length)).mapToInt(Integer::intValue)
                                .toArray()).toArray(int[][]::new), null);
        System.out.println(res.count);
    }
}