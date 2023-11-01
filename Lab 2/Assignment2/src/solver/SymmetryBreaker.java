package solver;

// Symmetry breaker wrapper
class SymmetryBreaker {

    // Weight of initial search (for instance, if model is built such that each solution counts as two,
    // then this value should be 2)
    Integer initialWeight;

    // Symmetry check function ((parameters, variable, element index, decision, domain) ->
    // is it a symmetrical state)
    SymmetryCheckFunction checkSymmetry;

    // Weight function ((parameters, variable, element index, decision, weight) ->
    // weight of current state/how many symmetries does it break)
    SymmetryWeightFunction calculateCountWeight;

    public SymmetryBreaker(SymmetryCheckFunction checkSymmetry, SymmetryWeightFunction calculateCountWeight,
                           Integer initialWeight) {
        this.checkSymmetry = checkSymmetry;
        this.calculateCountWeight = calculateCountWeight;
        this.initialWeight = initialWeight;
    }
}
