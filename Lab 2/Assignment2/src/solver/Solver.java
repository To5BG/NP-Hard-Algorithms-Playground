package solver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.Random;

public class Solver<E> {
    Map<String, Bind> parameterBinds = new HashMap<>(); // Binds a parameter name ({ value bind })
    Map<String, Object> parameters = new HashMap<>(); // Binds a parameter name to input values
    List<Pair> variableBinds = new ArrayList<>(); // Binds a variable name ({ domain bind, size
    // bind })
    List<Variable> variables = new ArrayList<>(); // Binds a variable name to wrapper class
    List<Constraint> constraints = new ArrayList<>(); // List of constraints to apply
    Integer best; // Min/max score for MIN/MAX problems
    Problem solType; // Type of problem to solve
    CSolution<E> sol; // Solution object - cache in case of repetitive solver queries
    Function<Map<String, Integer[]>, E> transform; // Transform solved variables in desired form
    Function<E, Integer> eval; // Evaluate weight in case of MIN/MAX solve
    Integer[] decisions; // List of decisions to take
    Triple[] index; // Maps decision to (variable, varIdx) pair
    SymmetryBreaker sym; // Bound symmetry breaker
    Boolean randomize = false, constrH = false; // Random decisions / most-constrained heuristic

    public Solver<E> addParameter(String name, Bind p) {
        this.parameterBinds.put(name, p);
        return this;
    }

    public Solver<E> addVariable(String name, VariableBind value) {
        variableBinds.add(new Pair(name, value));
        return this;
    }

    public Solver<E> addConstraint(String var, String option, Integer arg) {
        constraints.add(new Constraint(var, option, arg));
        return this;
    }

    public Solver<E> addConstraint(String var, PentaFunction<Map<String, Object>, Integer, Integer, Integer, BitSet[][], Boolean> pf) {
        constraints.add(new Constraint(var, pf));
        return this;
    }

    public Solver<E> addConstraint(List<String> par, List<String> var,
                                   BiFunction<List<Object>, List<Integer[]>, Boolean> constr,
                                   PentaFunction<Map<String, Object>, Integer, Integer, Integer, BitSet[][], Boolean> pf) {
        constraints.add(new Constraint(par, var, constr, pf));
        return this;
    }

    public Solver<E> setVariableSelection(Boolean randomize, Boolean most_constrained) {
        this.randomize = randomize;
        this.constrH = most_constrained;
        return this;
    }

    public Solver<E> addSymmetryBreaker(
            PentaFunction<Map<String, Object>, Integer, Integer, Integer, List<Integer>, Boolean> checkSymmetry,
            PentaFunction<Map<String, Object>, Integer, Integer, Integer, Integer, Integer> calculateCountWeight,
            Integer initialWeight) {
        sym = new SymmetryBreaker(checkSymmetry, calculateCountWeight, initialWeight);
        return this;
    }

    // Loads model by binding input values to parameters and variables
    private int loadModel(Map<String, Object> model) {
        int total = 0;
        // If bind is null, simply set value, else evaluate value with a function
        for (Map.Entry<String, Bind> e : parameterBinds.entrySet()) {
            if (e.getValue() == null) parameters.put(e.getKey(), model.get(e.getKey()));
            else parameters.put(e.getKey(), e.getValue().apply(parameters));
        }
        // Evaluate domain and value
        for (Pair e : variableBinds) {
            List<Integer> dom = e.right.applyDomain(parameters);
            // Fallacious design
            if (dom.isEmpty())
                throw new RuntimeException("Domain must be non-empty.");
            if (new HashSet<>(dom).size() != dom.size())
                throw new RuntimeException("Domain must contain unique values.");
            // 'decrease' propagator requires sorted domain
            dom.sort(Comparator.naturalOrder());
            Object val = e.right.apply(parameters);
            Integer[] v;
            // Either an array, or a single value (also represented as array for simplicity)
            if (val instanceof Integer[]) v = (Integer[]) val;
            else v = new Integer[]{(Integer) val};
            total += v.length;
            variables.add(new Variable(e.left, dom, v));
        }
        return total;
    }

    // Propagate all constraints that have a propagator
    private boolean propagate(Integer var, Integer idx, Integer decision, BitSet[][] domains) {
        for (Constraint e : this.constraints) {
            if (e.propFunc != null && e.propFunc.apply(this.parameters, var, idx, decision, domains)) return true;
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
        List<Triple> idxList = new ArrayList<>();
        for (int varIdx = 0; varIdx < variables.size(); varIdx++) {
            Variable v = variables.get(varIdx);
            for (int elIdx = 0; elIdx < v.val.length; elIdx++)
                idxList.add(new Triple(v.name, varIdx, elIdx));
        }
        this.index = idxList.toArray(Triple[]::new);
    }

    // Public solve - load model and initialization
    public CSolution<E> solve(Map<String, Object> model, Problem sol, Function<Map<String, Integer[]>, E> transform,
                              Function<E, Integer> eval) {
        // If already computed, return old solution
        if (this.sol != null && this.solType == sol) return this.sol;
        // Find parameters/precompute variables by loading model
        int total = loadModel(model);
        this.solType = sol;
        this.sol = new CSolution<>();
        this.transform = transform;
        this.eval = eval;
        this.best = (this.solType == Problem.MAX) ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        // Map variable names to initial values
        Map<String, Integer[]> vals = this.variables.stream().collect(Collectors.toMap(v -> v.name, v -> v.val));
        this.indexVariables();
        // Create decisions and preset arrays
        List<Triple> preset = new ArrayList<>();
        this.decisions = IntStream.range(0, total).filter(i -> {
            Triple ind = index[i];
            if (vals.get(ind.var)[ind.eid] == Integer.MIN_VALUE) return true;
            preset.add(ind);
            return false;
        }).boxed().toArray(Integer[]::new);
        // Randomize if enabled - copied java.util.Collections.shuffle() and repurposed for array
        if (randomize) {
            Random rand = new Random();
            for (int i = decisions.length; i > 1; i--) {
                int temp = decisions[i - 1], j = rand.nextInt(i);
                decisions[i - 1] = decisions[j];
                decisions[j] = temp;
            }
        }
        // Map variable names to possible domains, for every single list element
        BitSet[][] domains = this.variables.stream().map(v ->
                IntStream.range(0, v.val.length).mapToObj(i -> {
                    BitSet b = new BitSet(v.domain.size());
                    b.flip(0, b.size());
                    return b;
                }).toArray(BitSet[]::new)
        ).toArray(BitSet[][]::new);
        // If some values pre-set, already attempt propagation over them
        preset.forEach(t -> propagate(t.varIdx, t.eid,
                variables.get(t.varIdx).domain.indexOf(vals.get(t.var)[t.eid]), domains));
        // Track solve time
        long time = System.currentTimeMillis();
        // Start solution
        solve(vals, domains, 0, (this.sym == null) ? 1 : this.sym.initialWeight);
        System.out.println("Solved in: " + (System.currentTimeMillis() - time) + "ms");
        return this.sol;
    }

    // Private solve - recursive part of search
    private void solve(Map<String, Integer[]> values, BitSet[][] domains, int level, int weight) {
        // If all variables have been picked, check for satisfiability
        if (level == this.decisions.length) {
            if (!testSatisfy(values)) return;
            // If count -> skip transforming/adding entry
            if (this.solType != Problem.COUNT) {
                // Transform entry and add to solution pool
                E transformed = this.transform.apply(values);
                if (this.solType == Problem.SATISFY || this.solType == Problem.ALL)
                    this.sol.solutions.add(transformed);
                    // Evaluate entry if MIN/MAX problem and conditionally add to solution pool
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
            Triple currTriple = index[decisions[level]];
            String nextVar = currTriple.var;
            Integer nextVarIdx = currTriple.varIdx;
            Integer elIndex = currTriple.eid;
            BitSet currDomain = domains[nextVarIdx][elIndex];
            Variable var = variables.get(nextVarIdx);
            // For each possible value in domain, build next state
            for (int i = 0; i < var.domain.size(); i++) {
                // Early return if satisfy is solved
                if (solType == Problem.SATISFY && sol.count >= 1) break;
                // Skip invalidated decisions
                if (!currDomain.get(i)) continue;
                // If symmetric, skip branch generation
                if (sym != null && sym.checkSymmetry.apply(this.parameters, nextVarIdx, elIndex, i, var.domain))
                    continue;
                // Make decision
                values.get(nextVar)[elIndex] = var.domain.get(i);
                // New domain for future children
                BitSet[][] newDomain = new BitSet[domains.length][];
                for (int ii = 0; ii < domains.length; ii++) {
                    BitSet[] prev = domains[ii];
                    BitSet[] res = new BitSet[prev.length];
                    if (ii == nextVarIdx) {
                        for (int j = 0; j < prev.length; j++) {
                            if (j == elIndex || prev[j] == null) res[j] = null;
                            else res[j] = (BitSet) prev[j].clone();
                        }
                    } else {
                        for (int j = 0; j < prev.length; j++) {
                            if (prev[j] == null) res[j] = null;
                            else res[j] = (BitSet) prev[j].clone();
                        }
                    }
                    newDomain[ii] = res;
                }
                // If one of the domains becomes empty, then impossible to solve -> skip branch
                if (propagate(nextVarIdx, elIndex, i, newDomain)) continue;
                // Variable selection -> most-constrained-variable heuristic
                if (constrH) Arrays.sort(this.decisions, level + 1, this.decisions.length,
                        Comparator.comparing(n -> {
                            Triple curr = index[n];
                            return newDomain[curr.varIdx][curr.eid].cardinality();
                        }));
                // Continue with next decisions
                solve(values, newDomain, level + 1, (sym == null) ? weight :
                        sym.calculateCountWeight.apply(this.parameters, nextVarIdx, elIndex, i, weight));
            }
        }
    }
}