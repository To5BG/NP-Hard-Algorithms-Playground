package solver;

import java.util.Map;

// Symmetry breaker wrapper
class SymmetryBreaker {

    // Weight of initial search (for instance, if model is built such that each solution counts as two,
    // then this value should be 2)
    Integer initialWeight;

    // Symmetry check function ((node, decision) -> is it a symmetrical state)
    TriFunction<Node, Integer, Map<String, Object>, Boolean> checkSymmetry;

    // Weight function ((node, decision) -> weight of current state/how many symmetries does it break)
    TriFunction<Node, Integer, Map<String, Object>, Integer> calculateCountWeight;

    public SymmetryBreaker(TriFunction<Node, Integer, Map<String, Object>, Boolean> checkSymmetry,
                           TriFunction<Node, Integer, Map<String, Object>, Integer> calculateCountWeight,
                           Integer initialWeight) {
        this.checkSymmetry = checkSymmetry;
        this.calculateCountWeight = calculateCountWeight;
        this.initialWeight = initialWeight;
    }
}
