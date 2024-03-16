package solver;

import java.util.List;

// Symmetry breaker wrapper
class SymmetryBreaker {
    Integer initialWeight; // Weight of root branch (based on whether provided model eliminates symmetries)
    PentaFunction<List<Integer>, Boolean> checkSymmetry;
    // (params, var_idx, element_idx, decision, domain) -> is it a symmetry
    PentaFunction<Integer, Integer> calculateCountWeight;
    // (params, var_idx, element_idx, decision, weight) -> weight of branch

    public SymmetryBreaker(PentaFunction<List<Integer>, Boolean> checkSymmetry,
                           PentaFunction<Integer, Integer> calculateCountWeight, Integer initialWeight) {
        this.checkSymmetry = checkSymmetry;
        this.calculateCountWeight = calculateCountWeight;
        this.initialWeight = initialWeight;
    }
}