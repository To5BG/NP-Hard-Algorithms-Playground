import solver.CSolution;
import solver.Constraint;
import solver.Problem;
import solver.PropagatorFunction;
import solver.Solver;
import solver.VariableBind;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Sudoku {
    public static void main(String[] args) {

        int[][] grid = new int[][]{
                new int[]{5, -1, 9, 7, 6, -1, -1, -1, 2},
                new int[]{3, 8, 6, -1, -1, 9, 4, 7, 5},
                new int[]{4, 2, 7, 3, 5, 8, 9, -1, 6},
                new int[]{-1, 7, 1, 5, 8, 3, 2, 6, 4},
                new int[]{-1, 3, 8, 4, 7, -1, 1, -1, 9},
                new int[]{2, 4, 5, 9, -1, 6, 7, 8, -1},
                new int[]{1, -1, -1, 8, 3, 5, 6, 4, 7},
                new int[]{-1, 5, 4, 6, 9, -1, 3, -1, 1},
                new int[]{-1, 6, -1, 2, 4, 1, 5, -1, 8},
        };

        // Model board as N arrays of size N, i-th row called 'puzzle_i'
        List<String> fullVar = IntStream.range(0, grid.length).mapToObj(i -> "puzzle_" + i)
                .collect(Collectors.toList());

        Solver<int[][]> solver = new Solver<int[][]>().addParameter("N", null);

        // Apply the row-based constraints for each row separately
        for (int i = 0; i < grid.length; i++) {
            int finalI = i;
            solver.addVariable("puzzle_" + i, new VariableBind(
                            List.of("N"), j -> IntStream.rangeClosed(1, (Integer) j.get(0))
                            .boxed().collect(Collectors.toList()),
                            List.of("N"), j -> new Integer[(Integer) j.get(0)]))
                    // Setter constraint -> any provided values must be enforced on domains
                    .addConstraint("puzzle_" + i, new PropagatorFunction() {
                        @Override
                        public Boolean apply(Map<String, Object> params, String var, Integer idx, Integer decision,
                                             Map<String, Integer[][]> domains) {
                            Integer[][] v = domains.get(var);
                            for (int i = 0; i < v.length; i++)
                                if (grid[finalI][i] != -1) v[i] = new Integer[]{grid[finalI][i]};
                            return false;
                        }
                    })
                    // Sudoku rule #1: All numbers in a row are different
                    .addConstraint("puzzle_" + i, "alldiff", -1);
        }

        solver
                // Sudoku rule #2: All numbers in a column are different
                .addConstraint(new Constraint(List.of(), fullVar, (p, v) -> {
                    for (int col = 0; col < v.size(); col++)
                        for (int row1 = 0; row1 < v.size(); row1++)
                            for (int row2 = row1 + 1; row2 < v.size(); row2++)
                                if (v.get(row1)[col].equals(v.get(row2)[col])) return false;
                    return true;
                }, new PropagatorFunction() {
                    @Override
                    public Boolean apply(Map<String, Object> params, String var, Integer idx, Integer decision,
                                         Map<String, Integer[][]> domains) {
                        for (Map.Entry<String, Integer[][]> rowEntry : domains.entrySet()) {
                            if (rowEntry.getKey().equals(var)) continue;
                            if (findRepeatedEntries(idx, decision, rowEntry.getValue())) return true;
                        }
                        return false;
                    }
                }))
                // Sudoku rule #3: All numbers in an NxN area are different
                .addConstraint(new Constraint(List.of(), fullVar, (p, v) -> {
                    int n = (int) Math.sqrt(grid.length);
                    for (int i = 0; i < n; i++)
                        for (int j = 0; j < n; j++) {
                            int finalJ = j;
                            List<Integer> section = IntStream.range(i * n, (i + 1) * n).flatMap(ii ->
                                            Arrays.stream(grid[ii], finalJ * n,
                                                    (finalJ + 1) * n)).filter(e -> e != -1)
                                    .boxed().collect(Collectors.toList());
                            if (section.stream().distinct().count() != section.size()) return false;
                        }
                    return true;
                }, new PropagatorFunction() {
                    @Override
                    public Boolean apply(Map<String, Object> params, String var, Integer idx, Integer decision,
                                         Map<String, Integer[][]> domains) {
                        Integer row = Integer.parseInt(var.split("_")[1]),
                                n = (int) Math.sqrt((Integer) params.get("N"));
                        // find NxN region of decided variable
                        int i = row / n, j = idx / n;
                        for (Map.Entry<String, Integer[][]> rowEntry : domains.entrySet()) {
                            Integer i2 = Integer.parseInt(rowEntry.getKey().split("_")[1]) / n;
                            if (rowEntry.getKey().equals(var) || !i2.equals(i)) continue;
                            for (int j2 = j * n; j2 < (j + 1) * n; j2++)
                                if (findRepeatedEntries(j2, decision, rowEntry.getValue())) return true;
                        }
                        return false;
                    }
                }));

        Map<String, Object> model = new HashMap<>();
        model.put("N", grid.length);

        CSolution<int[][]> res = solver.solve(model, Problem.ALL, m ->
                IntStream.range(0, grid.length).mapToObj(i ->
                        Arrays.stream(Arrays.copyOf(m.get("puzzle_" + i), grid.length)).mapToInt(Integer::intValue)
                                .toArray()).toArray(int[][]::new), null);

        System.out.println(res.count);
        assert res.solutions.size() == 1;
        int[][] ss = res.solutions.get(0);
        for (int[] s : ss) {
            for (int i : s) System.out.print(i + " ");
            System.out.println();
        }
    }

    private static boolean findRepeatedEntries(Integer idx, Integer decision, Integer[][] rowEntry) {
        Integer[] entry = rowEntry[idx];
        for (int i = 0; i < entry.length; i++)
            if (entry[i].equals(decision)) entry[i] = Integer.MIN_VALUE;
        return Arrays.stream(entry).noneMatch(i -> i != Integer.MIN_VALUE);
    }
}