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
    //
    Integer total, minmaxScore;
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

    // Loads model by binding input values to parameters
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
        if (next == decisions.size() || isEmpty) return curr;
        Pair currPair = getNext(decisions.get(next));
        curr.nextValDecision = (String) currPair.l;
        curr.idx = (Integer) currPair.r;
        curr.domain = new ArrayList<>(domains.get(curr.nextValDecision).get(curr.idx));
        for (int i = 0; i < curr.domain.size(); i++) {
            if (symmetries != null && symmetries.checkSymmetry.apply(curr, i)) continue;
            Map<String, List<List<Integer>>> ndomain = domains.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue()
                            .stream().map(ArrayList::new).collect(Collectors.toList())));
            curr.children.add(buildTree(curr, next + 1, ndomain,
                    propagate(curr.nextValDecision, curr.idx, curr.domain.get(i), ndomain),
                    (symmetries == null) ? curr.weight : symmetries.calculateCountWeight.apply(curr, i)));
        }
        return curr;
    }

    private boolean testSatisfy(Map<String, Integer[]> test) {
        for (Constraint c : this.constraints) if (!c.apply(this.parameters, test)) return false;
        return true;
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
        this.minmaxScore = (this.solType == Problem.MAX) ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        this.total = variables.values().stream().map(v -> v.value.length).reduce(0, Integer::sum);
        //this.total = this.decisions.size();
        this.decisions = IntStream.range(0, total).boxed().collect(Collectors.toList());
        Collections.shuffle(this.decisions);
        Map<String, List<List<Integer>>> domain = this.variables.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> {
                    Variable var = e.getValue();
                    return IntStream.range(0, var.value.length).mapToObj(i -> new ArrayList<>(var.domain))
                            .collect(Collectors.toList());
                }));

        long a = System.currentTimeMillis();
        this.root = buildTree(null, 0, domain, false, (symmetries == null) ? 1 : symmetries.initialWeight);
        Map<String, Integer[]> init = this.variables.entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey, i -> i.getValue().value));
        solve(this.root, init, 0);
        System.out.println("Solved in: " + (System.currentTimeMillis() - a) + "ms");
        return this.sol;
    }

    private void solve(Node curr, Map<String, Integer[]> m, int level) {
        if (solType == Problem.SATISFY && sol.count >= 1) return;
        if (level == total) {
            if (testSatisfy(m)) {
                if (this.solType != Problem.COUNT) {
                    E transformed = this.transform.apply(m);
                    switch (this.solType) {
                        case SATISFY:
                            this.sol.solutions.add(transformed);
                            break;
                        case ALL:
                            this.sol.solutions.add(transformed);
                            break;
                        case COUNT:
                            break;
                        case MIN:
                            Integer scr = this.eval.apply(transformed);
                            if (scr < this.minmaxScore) {
                                this.sol.solutions.set(0, transformed);
                                this.minmaxScore = scr;
                            }
                            break;
                        case MAX:
                            Integer scrr = this.eval.apply(transformed);
                            if (scrr > this.minmaxScore) {
                                this.sol.solutions.set(0, transformed);
                                this.minmaxScore = scrr;
                            }
                    }
                }
                this.sol.count += curr.weight;
            } else return;
        }
        int t = curr.children.size();
        for (AtomicInteger i = new AtomicInteger(0); i.get() < t; i.getAndIncrement()) {
            m.computeIfPresent(curr.nextValDecision, (a, j) -> {
                j[curr.idx] = curr.domain.get(i.get());
                return j;
            });
            solve(curr.children.remove(0), m, level + 1);
        }
    }
}