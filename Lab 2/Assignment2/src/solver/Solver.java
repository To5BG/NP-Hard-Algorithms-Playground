package solver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    // Starting node root
    Node root;
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
    List<Integer> decisions;
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

    // helper for finding next variable within several variable arrays
    private Pair getNext(int s) {
        int varIndex = 0, varNum = 0;
        while (true) {
            if (varNum == this.variableNames.size()) break;
            int next = this.variables.get(this.variableNames.get(varNum++)).value.length;
            if (varIndex + next > s) break;
            varIndex += next;
        }
        String name = this.variableNames.get(varNum - 1);
        return new Pair(name, s - varIndex);
    }

    private Node buildTree(Node par, int next, Map<String, Integer[][]> domains, boolean isEmpty, int w) {
        Node curr = new Node(par);
        curr.weight = w;
        // child node -> all decisions done
        if (next == decisions.size() || isEmpty) return curr;
        // determine next variable to decide on (pick array and element)
        Pair currPair = getNext(decisions.get(next));
        curr.nextVarDecision = (String) currPair.l;
        curr.elementIndex = (Integer) currPair.r;
        // clone domain
        Integer[] oldDomain = domains.get(curr.nextVarDecision)[curr.elementIndex];
        curr.domain = Arrays.copyOf(oldDomain, oldDomain.length);
        // for each element in domain
        for (int i = 0; i < curr.domain.length; i++) {
            if (curr.domain[i] == Integer.MIN_VALUE) continue;
            // if symmetric, skip tree generation
            if (sym != null && sym.checkSymmetry.apply(curr, i, this.parameters)) continue;
            // clone domain again? TODO: look into that for optimization, I assume shallow clones would also work
            Map<String, Integer[][]> newDomain = domains.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, v -> Arrays.stream(v.getValue())
                            .map(arr -> Arrays.copyOf(arr, arr.length)).toArray(Integer[][]::new)));
            // build successor tree, and add as child
            curr.children.add(buildTree(curr, next + 1, newDomain,
                    propagate(curr.nextVarDecision, curr.elementIndex, curr.domain[i], newDomain),
                    (sym == null) ? curr.weight : sym.calculateCountWeight.apply(curr, i, this.parameters)));
        }
        return curr;
    }

    public CSolution<E> solve(Map<String, Object> model, Problem sol, Function<Map<String, Integer[]>, E> transform,
                              Function<E, Integer> eval) {
        // If already computed, return old solution
        if (this.sol != null && this.solType != sol) return this.sol;
        loadModel(model);
        this.solType = sol;
        this.sol = new CSolution<>();
        this.transform = transform;
        this.eval = eval;
        this.best = (this.solType == Problem.MAX) ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        this.decisions = IntStream.range(0, total).boxed().collect(Collectors.toList());
        // Randomize decision
        //Collections.shuffle(this.decisions);
        // map variable names to possible domains, for every single list element
        Map<String, Integer[][]> domains = this.variables.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> {
                    Variable var = e.getValue();
                    return IntStream.range(0, var.value.length).mapToObj(i -> var.domain.toArray(Integer[]::new))
                            .toArray(Integer[][]::new);
                }));
        // track solve time
        long time = System.currentTimeMillis();
        // build the tree to traverse
        this.root = buildTree(null, 0, domains, false, (sym == null) ? 1 : sym.initialWeight);
        // map variable names to initial values
        Map<String, Integer[]> init = this.variables.entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey, i -> i.getValue().value));
        // start traversing
        solve(this.root, init, 0);
        System.out.println("Solved in: " + (System.currentTimeMillis() - time) + "ms");
        return this.sol;
    }

    // Tests all constraints with given parameters and picked variables
    private boolean testSatisfy(Map<String, Integer[]> test) {
        for (Constraint c : this.constraints) if (!c.apply(this.parameters, test)) return false;
        return true;
    }

    private void solve(Node curr, Map<String, Integer[]> pickedValues, int level) {
        if (solType == Problem.SATISFY && sol.count >= 1) return;
        // if all variables have been picked, check for satisfiability
        if (level == total) {
            if (!testSatisfy(pickedValues)) return;
            // if count -> skip transforming/adding entry
            if (this.solType != Problem.COUNT) {
                // transform entry and add to solution pool
                E transformed = this.transform.apply(pickedValues);
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
            this.sol.count += curr.weight;
        }
        else {
            if (curr.domain == null) return;
            int nextChild = 0;
            // for each of the remaining domains
            for (int i = 0; i < curr.domain.length; i++) {
                if (curr.domain[i] == Integer.MIN_VALUE) continue;
                if (nextChild >= curr.children.size()) break;
                // get domain of next variable
                int finalI = i;
                pickedValues.computeIfPresent(curr.nextVarDecision, (varName, varValue) -> {
                    varValue[curr.elementIndex] = curr.domain[finalI];
                    return varValue;
                });
                // and solve for that
                solve(curr.children.get(nextChild++), pickedValues, level + 1);
            }
        }
    }
}