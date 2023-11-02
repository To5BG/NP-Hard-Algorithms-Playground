package solver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Solver<E> {

    // Binds a parameter name to a bind for its value
    Map<String, Bind> parameterBinds;
    // Binds a parameter name to input values
    Map<String, Object> parameters;
    // Binds a variable name to a variable bind ({ domain bind, size bind })
    Map<String, VariableBind> variableBinds;
    // Binds a variable name to wrapper class
    Map<String, Variable> variables;
    // Store variable names separate for optimization
    List<String> variableNames;
    // List of constraints to apply
    List<Constraint> constraints;
    // Total number of variables and min/max score for MIN/MAX problems
    Integer total, best;
    // Type of problem to solve
    Problem solType;
    // Solution object - cache in case of repetitive solver queries
    CSolution<E> sol;
    // Transform solved variables in desired form
    Function<Map<String, Integer[]>, E> transform;
    // Evaluate weight in case of MIN/MAX solve
    Function<E, Integer> eval;
    // List of decisions to take
    Integer[] decisions;
    // Maps decision to (variable, varIdx) pair
    Pair[] index;
    // Bound symmetry breaker
    SymmetryBreaker sym;

    public Solver() {
        this.parameterBinds = new HashMap<>();
        this.variableBinds = new HashMap<>();
        this.parameters = new HashMap<>();
        this.variables = new HashMap<>();
        this.variableNames = new ArrayList<>();
        this.constraints = new ArrayList<>();
    }

    public Solver<E> addParameter(String name, Bind p) {
        this.parameterBinds.put(name, p);
        return this;
    }

    public Solver<E> addVariable(String name, VariableBind value) {
        variableBinds.put(name, value);
        return this;
    }

    public Solver<E> addConstraint(String var, String option, Integer arg) {
        constraints.add(new Constraint(var, option, arg));
        return this;
    }

    public Solver<E> addConstraint(String var, PropagatorFunction pf) {
        constraints.add(new Constraint(var, pf));
        return this;
    }

    public Solver<E> addConstraint(Constraint constr) {
        constraints.add(constr);
        return this;
    }

    public void addSymmetryBreaker(SymmetryCheckFunction checkSymmetry, SymmetryWeightFunction calculateCountWeight,
                                   Integer initialWeight) {
        sym = new SymmetryBreaker(checkSymmetry, calculateCountWeight, initialWeight);
    }

    // Loads model by binding input values to parameters and variables
    private void loadModel(Map<String, Object> model) {
        this.total = 0;
        // If bind is null, simply set value, else evaluate value with a function
        for (Map.Entry<String, Bind> e : parameterBinds.entrySet()) {
            if (e.getValue() == null) parameters.put(e.getKey(), model.get(e.getKey()));
            else parameters.put(e.getKey(), e.getValue().apply(parameters));
        }
        // Evaluate domain and value
        for (Map.Entry<String, VariableBind> e : variableBinds.entrySet()) {
            List<Integer> dom = e.getValue().applyDomain(parameters);
            Object val = e.getValue().apply(parameters);
            Integer[] v;
            if (val instanceof Integer[]) v = (Integer[]) val;
            else v = new Integer[]{(Integer) val};
            this.total += v.length;
            Arrays.fill(v, Integer.MIN_VALUE);
            variables.put(e.getKey(), new Variable(dom, v));
            variableNames.add(e.getKey());
        }
    }

    // Propagate all constraints that have a propagator
    private boolean propagate(String var, Integer idx, Integer decision, Map<String, Integer[][]> domains) {
        boolean isEmpty = false;
        for (Constraint e : this.constraints) {
            if (isEmpty) break;
            if (e.variableNames.contains(var) && e.propFunc != null)
                isEmpty = e.propFunc.apply(this.parameters, var, idx, decision, domains);
        }
        return isEmpty;
    }

    // Helper for indexing variables (turned every combined index into (variable, varIdx) pair
    private void indexVariables() {
        List<Pair> idx = new ArrayList<>();
        for (String var : this.variableNames) {
            int varLength = this.variables.get(var).value.length;
            for (int varIdx = 0; varIdx < varLength; varIdx++)
                idx.add(new Pair(var, varIdx));
        }
        this.index = idx.toArray(Pair[]::new);
    }

    // Tests all constraints with given parameters and picked variables
    private boolean testSatisfy(Map<String, Integer[]> test) {
        for (Constraint c : this.constraints) if (!c.apply(this.parameters, test)) return false;
        return true;
    }

    public CSolution<E> solve(Map<String, Object> model, Problem sol, Function<Map<String, Integer[]>, E> transform,
                              Function<E, Integer> eval) {
        // If already computed, return old solution
        if (this.sol != null && this.solType != sol) return this.sol;
        // Find parameters/precompute variables by loading model
        loadModel(model);
        this.solType = sol;
        this.sol = new CSolution<>();
        this.transform = transform;
        this.eval = eval;
        this.best = (this.solType == Problem.MAX) ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        // Create decisions
        this.decisions = IntStream.range(0, total).boxed().toArray(Integer[]::new);
        this.indexVariables();
        // Map variable names to possible domains, for every single list element
        Map<String, Integer[][]> domains = this.variables.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> {
                    Variable var = e.getValue();
                    return IntStream.range(0, var.value.length).mapToObj(i -> var.domain.toArray(Integer[]::new))
                            .toArray(Integer[][]::new);
                }));
        // Map variable names to initial values
        Map<String, Integer[]> init = this.variables.entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey, i -> i.getValue().value));
        // Track solve time
        long time = System.currentTimeMillis();
        // Start solution
        solve(init, domains, 0, (this.sym == null) ? 1 : this.sym.initialWeight);
        System.out.println("Solved in: " + (System.currentTimeMillis() - time) + "ms");
        return this.sol;
    }

    private void solve(Map<String, Integer[]> values, Map<String, Integer[][]> domains, int level, int weight) {
        // If all variables have been picked, check for satisfiability
        if (level == total) {
            if (!testSatisfy(values)) return;
            // If count -> skip transforming/adding entry
            if (this.solType != Problem.COUNT) {
                // Transform entry and add to solution pool
                E transformed = this.transform.apply(values);
                switch (this.solType) {
                    case SATISFY:
                    case ALL:
                        this.sol.solutions.add(transformed);
                        break;
                    case MIN:
                    case MAX:
                        Integer scr = this.eval.apply(transformed);
                        if ((this.solType == Problem.MIN) ? scr < this.best : scr > this.best) {
                            this.sol.solutions.set(0, transformed);
                            this.best = scr;
                        }
                        break;
                }
            }
            this.sol.count += weight;
        } else {
            // Find variable name and index within it for future use
            Pair currPair = index[decisions[level]];
            String nextVarDecision = currPair.var;
            Integer elementIndex = currPair.eid;
            Integer[] currDomain = domains.get(nextVarDecision)[elementIndex];
            // For each possible value in domain, build next state
            for (int i = 0; i < currDomain.length; i++) {
                // Early return if satisfy is solved
                if (solType == Problem.SATISFY && sol.count >= 1) break;
                // Skip invalidated decisions
                if (currDomain[i] == Integer.MIN_VALUE) continue;
                // If symmetric, skip branch generation
                if (sym != null && sym.checkSymmetry.apply(this.parameters, nextVarDecision, elementIndex,
                        currDomain[i], currDomain)) continue;
                int finalI = i;
                // Take decision
                values.computeIfPresent(nextVarDecision, (varName, varValue) -> {
                    varValue[elementIndex] = currDomain[finalI];
                    return varValue;
                });
                // New domain for future children
                AtomicInteger k = new AtomicInteger(0);
                Map<String, Integer[][]> newDomain = domains.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, v -> Arrays.stream(v.getValue())
                                .map(arr -> {
                                    if (elementIndex.equals(k.getAndIncrement())) return arr;
                                    return Arrays.copyOf(arr, arr.length);
                                }).toArray(Integer[][]::new)));
                // If one of the domains becomes empty, then impossible to solve -> skip branch
                if (propagate(nextVarDecision, elementIndex, currDomain[i], newDomain)) continue;
                // Continue with next decisions
                solve(values, newDomain, level + 1, (sym == null) ? weight : sym.calculateCountWeight.apply(
                        this.parameters, nextVarDecision, elementIndex, currDomain[i], weight));
            }
        }
    }
}