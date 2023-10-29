package solver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Solver<E> {

    // Binds a parameter name to a bind for its value
    Map<String, Bind> parameterBinds;
    // Binds a parameter name to input values
    public Map<String, Object> parameters;
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
    SymmetryBreaker symmetries;

    public Solver() {
        this.parameterBinds = new HashMap<>();
        this.variableBinds = new HashMap<>();
        this.parameters = new HashMap<>();
        this.variables = new HashMap<>();
        this.variableNames = new ArrayList<>();
        this.constraints = new ArrayList<>();
        this.solType = null;
    }

    public Solver<E> addParameter(String name, Bind p) {
        this.parameterBinds.put(name, p);
        return this;
    }

    public Solver<E> setVariableSelectionOrder(List<Integer> ord) {
        this.decisions = ord;
        this.total = this.decisions.size();
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

    public void addSymmetryBreaker(BiFunction<Node, Integer, Boolean> checkSymmetry,
                                   BiFunction<Node, Integer, Integer> calculateCountWeight,
                                   Integer initialWeight) {
        symmetries = new SymmetryBreaker(checkSymmetry, calculateCountWeight, initialWeight);
    }

    // Loads model by binding input values to parameters and variables
    private void loadModel(Map<String, Object> model) {
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
            Arrays.fill(v, Integer.MIN_VALUE);
            variables.put(e.getKey(), new Variable(dom, v));
            variableNames.add(e.getKey());
        }
    }

    // Propagate all constraints that have a propagator
    private boolean propagate(String var, Integer idx, Integer decision, Map<String, List<List<Integer>>> domains) {
        AtomicBoolean isEmpty = new AtomicBoolean(false);
        for (Constraint e : this.constraints)
            if (e.variableNames.contains(var) && e.propFunc != null)
                isEmpty.set(e.propFunc.apply(this.parameters, var, idx, decision, domains));
        return isEmpty.get();
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

    private Node buildTree(Node par, int next, Map<String, List<List<Integer>>> domains, boolean isEmpty, int w) {
        Node curr = new Node(par);
        curr.weight = w;
        // child node -> all decisions done
        if (next == decisions.size() || isEmpty) return curr;
        // determine next variable to decide on (pick array and element)
        Pair currPair = getNext(decisions.get(next));
        curr.nextVarDecision = (String) currPair.l;
        curr.elementIndex = (Integer) currPair.r;
        // clone domain
        curr.domain = new ArrayList<>(domains.get(curr.nextVarDecision).get(curr.elementIndex));
        // for each element in domain
        for (int i = 0; i < curr.domain.size(); i++) {
            // if symmetric, skip tree generation
            if (symmetries != null && symmetries.checkSymmetry.apply(curr, i)) continue;
            // clone domain again? TODO: look into that for optimization, I assume shallow clones would also work
            Map<String, List<List<Integer>>> ndomain = domains.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue()
                            .stream().map(ArrayList::new).collect(Collectors.toList())));
            // build successor tree, and add as child
            curr.children.add(buildTree(curr, next + 1, ndomain,
                    propagate(curr.nextVarDecision, curr.elementIndex, curr.domain.get(i), ndomain),
                    (symmetries == null) ? curr.weight : symmetries.calculateCountWeight.apply(curr, i)));
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
        this.total = variables.values().stream().map(v -> v.value.length).reduce(0, Integer::sum);
        this.decisions = IntStream.range(0, total).boxed().collect(Collectors.toList());
        // Randomize decision
        Collections.shuffle(this.decisions);
        // map variable names to possible domains, for every single list element
        Map<String, List<List<Integer>>> domain = this.variables.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> {
                    Variable var = e.getValue();
                    return IntStream.range(0, var.value.length).mapToObj(i -> new ArrayList<>(var.domain))
                            .collect(Collectors.toList());
                }));
        // track solve time
        long time = System.currentTimeMillis();
        // build the tree to traverse
        this.root = buildTree(null, 0, domain, false,
                (symmetries == null) ? 1 : symmetries.initialWeight);
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

    private void solve(Node curr, Map<String, Integer[]> m, int level) {
        if (solType == Problem.SATISFY && sol.count >= 1) return;
        // if all variables have been picked, check for satisfiability
        if (level == total) {
            if (!testSatisfy(m)) return;
            // if count -> skip transforming/adding entry
            if (this.solType != Problem.COUNT) {
                // transform entry and add to solution pool
                E transformed = this.transform.apply(m);
                switch (this.solType) {
                    case SATISFY:
                    case ALL:
                        this.sol.solutions.add(transformed);
                        break;
                    case MIN:
                    case MAX:
                        Integer scr = this.eval.apply(transformed);
                        boolean check = (this.solType == Problem.MIN) ? scr < this.best : scr > this.best;
                        if (check) {
                            this.sol.solutions.set(0, transformed);
                            this.best = scr;
                        }
                        break;
                }
            }
            this.sol.count += curr.weight;
        }
        int t = curr.children.size();
        // for each of the remaining domains
        for (AtomicInteger i = new AtomicInteger(0); i.get() < t; i.getAndIncrement()) {
            // get domain of next variable
            m.computeIfPresent(curr.nextVarDecision, (varName, varValue) -> {
                varValue[curr.elementIndex] = curr.domain.get(i.get());
                return varValue;
            });
            // and solve for that
            solve(curr.children.remove(0), m, level + 1);
        }
    }
}