package solver;

import java.util.function.BiFunction;

// Symmetry breaker wrapper
class SymmetryBreaker {

    // Weight of initial search (for instance, if model is built such that each solution counts as two,
    // then this value should be 2)
    Integer initialWeight;

    // Function that checks if a symmetry can be broken
    BiFunction<Node, Integer, Boolean> checkSymmetry;

    // Weight function for each branch (how much does each symmetry cut the search space)
    BiFunction<Node, Integer, Integer> calculateCountWeight;

    public SymmetryBreaker(BiFunction<Node, Integer, Boolean> checkSymmetry,
                           BiFunction<Node, Integer, Integer> calculateCountWeight, Integer initialWeight) {
        //this.bind = bind;
        this.checkSymmetry = checkSymmetry;
        this.calculateCountWeight = calculateCountWeight;
        this.initialWeight = initialWeight;
    }
}
