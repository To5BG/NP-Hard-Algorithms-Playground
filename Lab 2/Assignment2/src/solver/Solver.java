package solver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Solver<E> {
    Map<String, Bind> parameterBinds = new HashMap<>(); // Binds a parameter name ({ value bind })
    Map<String, Object> parameters = new HashMap<>(); // Binds a parameter name to input values
    Map<String, VariableBind> variableBinds = new HashMap<>(); // Binds a variable name ({ domain bind, size bind })
    Map<String, Variable> variables = new HashMap<>(); // Binds a variable name to wrapper class
    List<String> variableNames = new ArrayList<>(); // Store variable names separate for optimization
    List<Constraint> constraints = new ArrayList<>(); // List of constraints to apply
    Integer total, best; // Total number of variables and min/max score for MIN/MAX problems
    Problem solType; // Type of problem to solve
    CSolution<E> sol; // Solution object - cache in case of repetitive solver queries
    Function<Map<String, Integer[]>, E> transform; // Transform solved variables in desired form
    Function<E, Integer> eval; // Evaluate weight in case of MIN/MAX solve
    Integer[] decisions; // List of decisions to take
    Pair[] index; // Maps decision to (variable, varIdx) pair
    SymmetryBreaker sym; // Bound symmetry breaker
    Boolean randomize = false, constrH = false; // Random decisions / most-constrained heuristic

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

    public Solver<E> addConstraint(String var, PentaFunction<
            Map<String, Object>, String, Integer, Integer, Map<String, Integer[][]>, Boolean> pf) {
        constraints.add(new Constraint(var, pf));
        return this;
    }

    public Solver<E> addConstraint(List<String> par, List<String> var,
                                   BiFunction<List<Object>, List<Integer[]>, Boolean> constr, PentaFunction<
            Map<String, Object>, String, Integer, Integer, Map<String, Integer[][]>, Boolean> pf) {
        constraints.add(new Constraint(par, var, constr, pf));
        return this;
    }

    public Solver<E> setVariableSelection(Boolean randomize, Boolean most_constrained) {
        this.randomize = randomize;
        this.constrH = most_constrained;
        return this;
    }

    public Solver<E> addSymmetryBreaker(PentaFunction<Map<String, Object>, String, Integer, Integer, Integer[], Boolean>
                                                checkSymmetry,
                                        PentaFunction<Map<String, Object>, String, Integer, Integer, Integer, Integer>
                                                calculateCountWeight, Integer initialWeight) {
        sym = new SymmetryBreaker(checkSymmetry, calculateCountWeight, initialWeight);
        return this;
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
            dom.add(0, dom.size());
            Object val = e.getValue().apply(parameters);
            Integer[] v;
            if (val instanceof Integer[]) v = (Integer[]) val;
            else v = new Integer[]{(Integer) val};
            this.total += v.length;
            Arrays.fill(v, Integer.MIN_VALUE);
            variables.put(e.getKey(), new Variable(dom, v));
            variableNames.add(e.getKey());
        }
        Collections.sort(variableNames);
    }

    // Propagate all constraints that have a propagator
    private boolean propagate(String var, Integer idx, Integer decision, Map<String, Integer[][]> domains) {
        for (Constraint e : this.constraints) {
            if (e.variableNames.contains(var) && e.propFunc != null)
                if (e.propFunc.propagate(this.parameters, var, idx, decision, domains)) return true;
        }
        return false;
    }

    // Tests all constraints with given parameters and picked variables
    private boolean testSatisfy(Map<String, Integer[]> test) {
        for (Constraint c : this.constraints) if (!c.check(this.parameters, test)) return false;
        return true;
    }

    // Helper for indexing variables (turn every combined index into (variable, varIdx) pair
    private void indexVariables() {
        List<Pair> idxList = new ArrayList<>();
        for (String var : this.variableNames)
            for (int varIdx = 0; varIdx < this.variables.get(var).val.length; varIdx++)
                idxList.add(new Pair(var, varIdx));
        this.index = idxList.toArray(Pair[]::new);
    }

    public CSolution<E> solve(Map<String, Object> model, Problem sol, Function<Map<String, Integer[]>, E> transform,
                              Function<E, Integer> eval) {
        // If already computed, return old solution
        if (this.sol != null && this.solType == sol) return this.sol;
        // Find parameters/precompute variables by loading model
        loadModel(model);
        this.solType = sol;
        this.sol = new CSolution<>();
        this.transform = transform;
        this.eval = eval;
        this.best = (this.solType == Problem.MAX) ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        // Create decisions
        if (randomize) {
            List<Integer> decisions = IntStream.range(0, total).boxed().collect(Collectors.toList());
            Collections.shuffle(decisions);
            this.decisions = decisions.toArray(Integer[]::new);
        } else this.decisions = IntStream.range(0, total).boxed().toArray(Integer[]::new);
        this.indexVariables();
        // Track solve time
        long time = System.currentTimeMillis();
        // Start solution
        solve(// Map variable names to initial values
                this.variables.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, i -> i.getValue().val)),
                // Map variable names to possible domains, for every single list element
                this.variables.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> {
                    Variable var = e.getValue();
                    return IntStream.range(0, var.val.length).mapToObj(i -> var.domain.toArray(Integer[]::new))
                            .toArray(Integer[][]::new);
                })), 0, (this.sym == null) ? 1 : this.sym.initialWeight);
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
                if (this.solType == Problem.SATISFY || this.solType == Problem.ALL)
                    this.sol.solutions.add(transformed);
                else {
                    Integer scr = this.eval.apply(transformed);
                    if ((this.solType == Problem.MIN) ? scr < this.best : scr > this.best) {
                        this.sol.solutions = List.of(transformed);
                        this.best = scr;
                    }
                }
            }
            this.sol.count += weight;
        } else {
            // Find variable name and index within it for future use
            Pair currPair = index[decisions[level]];
            String nextVar = currPair.var;
            Integer elIndex = currPair.eid;
            Integer[] currDomain = domains.get(nextVar)[elIndex];
            // For each possible value in domain, build next state
            for (int i = 1; i < currDomain.length; i++) {
                // Early return if satisfy is solved
                if (solType == Problem.SATISFY && sol.count >= 1) break;
                // Skip invalidated decisions
                if (currDomain[i] == Integer.MIN_VALUE) continue;
                // If symmetric, skip branch generation
                if (sym != null && sym.checkSymmetry.propagate(this.parameters, nextVar, elIndex, currDomain[i],
                        currDomain)) continue;
                int finalI = i;
                // Take decision
                values.computeIfPresent(nextVar, (varName, varValue) -> {
                    varValue[elIndex] = currDomain[finalI];
                    return varValue;
                });
                // New domain for future children
                Map<String, Integer[][]> newDomain = domains.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, v -> Arrays.stream(v.getValue())
                                .map(arr -> Arrays.copyOf(arr, arr.length)).toArray(Integer[][]::new)));
                // If one of the domains becomes empty, then impossible to solve -> skip branch
                if (propagate(nextVar, elIndex, currDomain[i], newDomain)) continue;
                // Variable selection -> most-constrained-variable heuristic
                if (constrH) Arrays.sort(this.decisions, level + 1, total, Comparator.comparing(n -> {
                    Pair curr = index[n];
                    return newDomain.get(curr.var)[curr.eid][0];
                }));
                // Continue with next decisions
                solve(values, newDomain, level + 1, (sym == null) ? weight : sym.calculateCountWeight.propagate(
                        this.parameters, nextVar, elIndex, currDomain[i], weight));
            }
        }
    }
}