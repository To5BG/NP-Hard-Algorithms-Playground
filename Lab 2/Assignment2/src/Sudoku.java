import solver.Bind;
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

public class Sudoku {
    public static void main(String[] args) {

        int[][] grid = new int[][]{
                new int[]{3, -1, 9, 12, -1, 14, -1, 7, -1, -1, 19, -1, -1, 17, 13, 18, -1, 6, 1, 20, -1, 4, -1, 24, 23,},
                new int[]{-1, 11, 2, -1, 13, 9, -1, 8, 24, -1, 21, 6, -1, -1, -1, 17, -1, -1, 10, -1, -1, -1, -1, -1, -1,},
                new int[]{-1, -1, -1, -1, 4, 2, -1, 3, -1, 10, -1, -1, 18, -1, 11, 22, -1, -1, -1, 8, 14, 19, 17, 6, 9,},
                new int[]{-1, 21, -1, 6, -1, -1, 18, -1, -1, -1, -1, 4, -1, -1, -1, -1, 11, -1, -1, 5, 16, -1, 2, -1, -1,},
                new int[]{-1, 20, -1, -1, 10, 4, -1, 23, 6, 25, 3, -1, 8, -1, 14, 19, -1, -1, -1, -1, -1, -1, 13, 11, -1,},
                new int[]{13, 25, -1, -1, 17, 3, 2, 6, 10, 24, 23, 18, -1, -1, 21, -1, -1, 4, 8, -1, -1, 16, -1, 14, 5,},
                new int[]{6, -1, 18, 14, 23, -1, -1, -1, 13, 20, -1, -1, -1, 16, -1, -1, 5, 12, 17, 2, -1, 21, -1, -1, 24,},
                new int[]{-1, -1, -1, 3, 16, 1, 11, 22, 23, -1, 7, 20, 9, 6, 17, -1, 10, -1, -1, 14, -1, -1, -1, -1, 13,},
                new int[]{-1, 1, 7, -1, 20, -1, 14, -1, 17, -1, -1, -1, -1, -1, -1, 21, 3, -1, -1, -1, 2, -1, 18, 9, 6,},
                new int[]{22, -1, 8, 24, -1, 15, 9, 5, 21, 4, 13, 14, 19, 3, 2, 20, -1, 16, -1, -1, 25, 7, 23, -1, 17,},
                new int[]{1, 18, -1, 21, 6, 11, -1, 24, 5, -1, 16, 12, 15, 9, -1, 4, 14, -1, -1, 10, 8, -1, 20, 3, 22,},
                new int[]{23, 15, -1, -1, 24, -1, -1, 20, 9, -1, -1, -1, 14, 21, 6, 11, 18, 7, 12, 3, 4, 13, -1, 5, -1,},
                new int[]{12, 3, -1, -1, -1, -1, -1, 15, -1, -1, 20, 22, -1, -1, -1, 24, -1, -1, -1, 23, -1, 14, 21, 16, 11,},
                new int[]{-1, 19, -1, 10, 5, -1, 23, -1, 12, 16, 4, 24, -1, -1, -1, -1, -1, 20, 2, -1, 18, -1, -1, -1, -1,},
                new int[]{-1, 9, -1, -1, 8, 25, -1, -1, -1, -1, 10, -1, 11, 23, -1, 16, -1, 5, 15, 22, 24, -1, 12, -1, 7,},
                new int[]{20, -1, -1, -1, 11, 6, 19, -1, 3, 7, 2, 21, 5, -1, 12, -1, 1, -1, 4, 17, -1, -1, -1, 18, 14,},
                new int[]{25, 16, 24, 17, -1, 20, 12, -1, 11, -1, -1, -1, 10, -1, -1, -1, -1, 8, -1, -1, 23, -1, -1, 7, 4,},
                new int[]{4, 6, 15, 23, 1, 17, 24, -1, 14, -1, -1, 3, -1, 19, 16, -1, 12, 10, 9, -1, -1, 25, 22, -1, 8,},
                new int[]{-1, 12, 3, -1, 2, 5, 13, 4, 22, -1, -1, 25, 23, 15, -1, -1, -1, -1, 7, 19, 1, 24, 11, 17, 10,},
                new int[]{-1, 7, 14, 5, 19, -1, -1, 1, -1, -1, -1, -1, -1, 18, 24, -1, -1, -1, -1, 25, 13, 12, 6, 21, -1,},
                new int[]{5, -1, -1, 16, -1, 21, -1, 13, -1, -1, 14, 23, -1, -1, -1, -1, -1, -1, -1, 1, -1, 15, 7, -1, -1,},
                new int[]{24, -1, -1, 4, -1, -1, -1, 10, -1, -1, 5, -1, -1, -1, -1, -1, 9, 3, -1, 21, 12, 1, -1, -1, -1,},
                new int[]{10, -1, -1, 11, 25, 16, -1, -1, 15, 22, -1, -1, -1, 24, -1, 5, -1, -1, -1, 4, 3, -1, 8, -1, 18,},
                new int[]{-1, -1, 13, 20, 3, -1, -1, -1, -1, -1, 18, 1, 12, 25, 4, -1, -1, 15, 14, 16, -1, 10, -1, 2, 19,},
                new int[]{8, -1, 1, -1, 18, 23, 3, -1, -1, -1, 17, 16, 13, 10, 15, 25, 20, 19, 6, 12, -1, 5, 24, -1, 21,},
        };

        Map<String, Object> model = new HashMap<>();
        model.put("N", grid.length);

        // Model board as N arrays of size N, i-th row called 'puzzle_i'
        List<String> fullVar = IntStream.range(0, grid.length).mapToObj(i -> "puzzle_" + i)
                .collect(Collectors.toList());

        Solver<int[][]> solver = new Solver<int[][]>()
                .addParameter("N", null)
                .addParameter("n", new Bind(List.of("N"), (l) -> (int) Math.sqrt((int) l.get(0))))
                .setVariableSelection(false, true)
                // Setter constraint -> any provided values must be enforced on domains
                .addConstraint(List.of(), fullVar, (p, v) -> true, (params, var, idx, decision, domains) -> {
                    // Propagate only once
                    if (domains.get(var)[idx].cardinality() == 1) return false;
                    // Skip unfruitful decision chains
                    if (grid[Integer.parseInt(var.substring(7))][idx] != -1 &&
                            grid[Integer.parseInt(var.substring(7))][idx] != (decision + 1)) return true;
                    for (int n = 0; n < grid.length; n++) {
                        BitSet[] v = domains.get("puzzle_" + n);
                        for (int i = 0; i < v.length; i++)
                            if (grid[n][i] != -1) {
                                v[i].clear();
                                v[i].set(grid[n][i] - 1);
                            }
                    }
                    return false;
                });

        // Apply the row-based constraints for each row separately
        for (int i = 0; i < grid.length; i++) {
            solver.addVariable("puzzle_" + i, new VariableBind(
                            List.of("N"), j -> IntStream.rangeClosed(1, (Integer) j.get(0))
                            .boxed().collect(Collectors.toList()),
                            List.of("N"), j -> new Integer[(Integer) j.get(0)]))
                    // Sudoku rule #1: All numbers in a row are different
                    .addConstraint("puzzle_" + i, "alldiff", -1);
        }

        // Sudoku rule #2: All numbers in a column are different
        solver.addConstraint(List.of(), fullVar, (p, v) -> {
                    for (int col = 0; col < v.size(); col++)
                        for (int row1 = 0; row1 < v.size(); row1++)
                            for (int row2 = row1 + 1; row2 < v.size(); row2++)
                                if (v.get(row1)[col].equals(v.get(row2)[col])) return false;
                    return true;
                }, (params, var, idx, decision, domains) -> {
                    for (Map.Entry<String, BitSet[]> rowEntry : domains.entrySet()) {
                        BitSet curr = rowEntry.getValue()[idx];
                        if (rowEntry.getKey().equals(var)) continue;
                        curr.clear(decision);
                        if (curr.isEmpty()) return true;
                    }
                    return false;
                })
                // Sudoku rule #3: All numbers in an NxN area are different
                .addConstraint(List.of("n"), fullVar, (p, v) -> {
                    int n = (int) p.get(0);
                    for (int row = 0; row < n; row++)
                        for (int col = 0; col < n; col++) {
                            int finalJ = col;
                            List<Integer> section = IntStream.range(row * n, (row + 1) * n).boxed().flatMap(ii ->
                                    Arrays.stream(v.get(ii), finalJ * n,
                                            (finalJ + 1) * n)).collect(Collectors.toList());
                            if (section.stream().distinct().count() != section.size()) return false;
                        }
                    return true;
                }, (params, var, idx, decision, domains) -> {
                    int row = Integer.parseInt(var.substring(7)), n = (Integer) params.get("n");
                    // Find NxN region of decided variable
                    int rowIdx = row / n, colIdx = idx / n;
                    for (int row2 = 0; row2 < n * n; row2++) {
                        // Only update rows within same rowIdx (that are part of same section)
                        if (row2 / n != rowIdx || row == row2) continue;
                        BitSet[] curr = domains.get("puzzle_" + row2);
                        for (int col = colIdx * n; col < (colIdx + 1) * n; col++) {
                            curr[col].clear(decision);
                            if (curr[col].isEmpty()) return true;
                        }
                    }
                    return false;
                });

        CSolution<int[][]> res = solver.solve(model, Problem.SATISFY, m ->
                IntStream.range(0, grid.length).mapToObj(i ->
                        Arrays.stream(Arrays.copyOf(m.get("puzzle_" + i), grid.length)).mapToInt(Integer::intValue)
                                .toArray()).toArray(int[][]::new), null);

        System.out.println(res.count);
        int[][] ss = res.solutions.get(0);
        for (int[] s : ss) {
            for (int i : s) System.out.print(i + " ");
            System.out.println();
        }
    }
}