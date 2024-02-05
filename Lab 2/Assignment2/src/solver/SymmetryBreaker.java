package solver;

import java.util.List;
import java.util.Map;

// Symmetry breaker wrapper
class SymmetryBreaker {
    Integer initialWeight; // Weight of root branch (based on whether provided model eliminates symmetries)
    PentaFunction<Map<String, Object>, String, Integer, Integer, List<Integer>, Boolean> checkSymmetry;
    // (params, var, element index, decision, domain) -> is it a symmetry
    PentaFunction<Map<String, Object>, String, Integer, Integer, Integer, Integer> calculateCountWeight;
    // (params, var, element index, decision, weight) -> weight of branch

    public SymmetryBreaker(PentaFunction<Map<String, Object>, String, Integer, Integer, List<Integer>, Boolean>
                                   checkSymmetry,
                           PentaFunction<Map<String, Object>, String, Integer, Integer, Integer, Integer>
                                   calculateCountWeight, Integer initialWeight) {
        this.checkSymmetry = checkSymmetry;
        this.calculateCountWeight = calculateCountWeight;
        this.initialWeight = initialWeight;
    }
}