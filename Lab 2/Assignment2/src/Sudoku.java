import solver.Bind;
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
//                new int[]{-1,7,-1,-1,-1,-1,13,-1,-1,9,-1,1,17,10,8,-1,20,16,15,22,4,19,24,-1,14,},
//                new int[]{-1,-1,-1,15,20,19,11,16,5,-1,-1,13,-1,18,3,9,1,21,-1,-1,-1,17,8,2,-1,},
//                new int[]{17,11,-1,19,16,7,-1,24,-1,12,-1,4,20,-1,-1,14,5,10,2,13,6,-1,15,18,1,},
//                new int[]{1,-1,-1,22,9,-1,-1,-1,-1,-1,12,15,-1,25,11,23,-1,3,19,6,20,-1,16,-1,5,},
//                new int[]{21,5,-1,3,23,8,-1,1,20,15,19,14,16,6,24,17,7,25,11,4,10,12,22,-1,-1,},
//                new int[]{-1,16,-1,-1,13,1,-1,-1,23,8,9,7,6,3,-1,-1,14,12,21,20,24,11,19,4,18,},
//                new int[]{-1,6,25,9,10,-1,-1,-1,-1,-1,4,-1,18,-1,-1,11,-1,22,16,1,3,5,17,15,20,},
//                new int[]{24,21,20,2,-1,9,-1,5,4,-1,-1,16,-1,11,-1,6,3,-1,18,8,-1,25,7,14,-1,},
//                new int[]{-1,8,-1,11,19,20,-1,6,18,-1,-1,-1,25,17,-1,13,4,15,7,9,1,-1,23,-1,-1,},
//                new int[]{14,-1,3,-1,-1,-1,12,15,-1,11,24,-1,1,22,21,2,23,19,5,-1,9,10,-1,8,13,},
//                new int[]{25,24,10,-1,12,6,17,2,19,1,7,9,5,20,14,21,15,4,22,18,11,8,-1,-1,16,},
//                new int[]{6,19,21,18,8,-1,-1,4,11,23,-1,2,-1,24,-1,12,9,14,-1,17,22,-1,-1,20,10,},
//                new int[]{22,17,-1,-1,-1,18,7,8,13,-1,-1,25,12,-1,1,19,-1,-1,10,-1,5,14,-1,24,15,},
//                new int[]{-1,-1,-1,-1,5,14,-1,3,9,22,10,18,-1,21,-1,8,11,24,1,23,2,4,-1,12,17,},
//                new int[]{11,15,-1,14,2,-1,24,-1,-1,25,16,17,-1,4,22,20,13,5,3,-1,23,-1,18,9,-1,},
//                new int[]{7,-1,19,20,17,-1,-1,-1,24,18,-1,12,-1,-1,4,-1,-1,13,-1,2,-1,15,10,-1,21,},
//                new int[]{3,-1,23,-1,22,25,-1,17,1,16,18,24,9,-1,-1,15,-1,11,12,21,13,-1,5,6,-1,},
//                new int[]{15,25,11,-1,-1,-1,8,-1,6,7,2,10,21,13,17,3,-1,23,20,-1,14,-1,9,22,-1,},
//                new int[]{9,12,-1,6,21,2,20,22,15,4,25,19,23,14,5,24,-1,-1,8,10,-1,1,-1,16,-1,},
//                new int[]{8,2,16,5,24,3,9,-1,21,10,6,11,-1,-1,7,1,25,18,4,-1,-1,-1,20,17,12,},
//                new int[]{-1,23,17,8,15,4,1,-1,25,13,20,22,11,19,18,-1,21,2,-1,3,16,9,-1,5,-1,},
//                new int[]{20,3,5,16,-1,24,-1,11,2,21,-1,-1,-1,-1,-1,25,22,9,17,19,-1,13,4,-1,-1,},
//                new int[]{-1,9,6,21,25,-1,19,-1,10,5,-1,-1,24,-1,16,-1,-1,-1,-1,-1,-1,-1,-1,23,8,},
//                new int[]{18,1,2,-1,11,15,-1,20,17,14,-1,-1,4,5,9,-1,10,8,23,24,7,22,3,-1,25,},
//                new int[]{-1,22,-1,-1,7,-1,-1,-1,8,-1,-1,6,14,-1,25,-1,18,1,13,15,21,20,12,10,-1,},

                new int[]{10, -1, -1, 7, 13, 4, 16, -1, 11, 1, -1, -1, 12, -1, 6, -1,},
                new int[]{-1, 1, 16, 6, -1, 2, 10, 3, -1, 12, -1, -1, 15, 11, 5, -1,},
                new int[]{12, -1, 5, 3, 11, 9, 15, 1, 4, 6, 8, 2, 14, 10, 7, -1,},
                new int[]{-1, -1, 2, -1, 7, -1, -1, -1, 13, -1, -1, -1, 4, -1, -1, -1,},
                new int[]{-1, -1, 7, -1, 5, -1, 8, -1, 6, 13, 14, 10, 11, 12, 1, 15,},
                new int[]{15, 8, 10, 11, 12, 1, 13, 9, 3, 7, 2, 4, 6, -1, 16, 5,},
                new int[]{-1, -1, 13, 12, 14, 10, 11, 16, 8, 15, -1, -1, -1, -1, 4, 2,},
                new int[]{1, -1, -1, 5, -1, 7, 6, 2, 9, -1, -1, 16, -1, 8, -1, 10,},
                new int[]{-1, -1, -1, 13, -1, -1, -1, 6, 1, -1, 10, 11, 7, -1, 8, -1,},
                new int[]{5, 12, -1, 8, 1, 16, 7, 10, 14, 3, -1, 9, 2, 13, 15, 4,},
                new int[]{7, 16, 4, 1, 9, 11, 12, -1, -1, -1, -1, 13, 10, 6, 14, -1,},
                new int[]{6, 14, 9, 10, -1, 15, -1, 13, -1, -1, 7, -1, 16, -1, -1, -1,},
                new int[]{16, 9, 12, -1, 10, -1, 1, 11, 15, 14, -1, 7, 5, 3, -1, -1,},
                new int[]{13, 5, 6, 15, -1, -1, 2, 7, 10, 9, 3, -1, -1, 4, -1, 11,},
                new int[]{8, -1, 1, -1, 3, 12, 4, 15, -1, 2, 11, 6, 13, 16, 9, 7,},
                new int[]{11, 7, 3, -1, 6, 13, 9, 5, -1, 8, 4, -1, -1, 15, 10, 14,},
        };

        // Model board as N arrays of size N, i-th row called 'puzzle_i'
        List<String> fullVar = IntStream.range(0, grid.length).mapToObj(i -> "puzzle_" + i)
                .collect(Collectors.toList());

        Solver<int[][]> solver = new Solver<int[][]>()
                .addParameter("N", null)
                .addParameter("n", new Bind(List.of("N"), (l) -> (int) Math.sqrt((int) l.get(0))))
                .setVariableSelection(false, true);

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
                                if (grid[finalI][i] != -1)
                                    v[i] = new Integer[]{1, grid[finalI][i]};
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
                            if (findRepeatedEntries(rowEntry.getValue()[idx], decision)) return true;
                        }
                        return false;
                    }
                }))
                // Sudoku rule #3: All numbers in an NxN area are different
                .addConstraint(new Constraint(List.of("n"), fullVar, (p, v) -> {
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
                }, new PropagatorFunction() {
                    @Override
                    public Boolean apply(Map<String, Object> params, String var, Integer idx, Integer decision,
                                         Map<String, Integer[][]> domains) {
                        int row = Integer.parseInt(var.substring(7)), n = (Integer) params.get("n");
                        // Find NxN region of decided variable
                        int rowIdx = row / n, colIdx = idx / n;
                        for (int row2 = 0; row2 < n * n; row2++) {
                            // Only update rows within same rowIdx (that are part of same section)
                            if (row2 / n != rowIdx || row == row2) continue;
                            for (int col = colIdx * n; col < (colIdx + 1) * n; col++)
                                if (findRepeatedEntries(domains.get("puzzle_" + row2)[col], decision)) return true;
                        }
                        return false;
                    }
                }));

        Map<String, Object> model = new HashMap<>();
        model.put("N", grid.length);

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

    // Helper to remove repeating decisions on an entry
    private static boolean findRepeatedEntries(Integer[] entry, Integer decision) {
        for (int i = 1; i < entry.length; i++)
            if (entry[i].equals(decision)) {
                entry[i] = Integer.MIN_VALUE;
                if (--entry[0] == 0) return true;
            }
        return false;
    }
}