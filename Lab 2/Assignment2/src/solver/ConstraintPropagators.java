package solver;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConstraintPropagators {

    static PropagatorFunction get(String option, int arg) {
        switch (option) {
            case "alldiff":
                return alldiff(arg);
            case "decrease":
                return decrease();
            case "hi":
                return hi();
            case "h2":
                return hi2();
            default:
                return null;
        }
    }

    // Implementation of alldiff global constraint
    // arg -> what value is allowed to repeat
    static PropagatorFunction alldiff(int arg) {
        return new PropagatorFunction() {
            @Override
            public Boolean apply(Map<String, Object> params, String var, Integer idx, Integer decision,
                                 Map<String, Integer[][]> domains) {
                if (decision.equals(arg)) return false;
                AtomicBoolean res = new AtomicBoolean(false);
                domains.computeIfPresent(var, (k, v) -> {
                    for (int i = 0; i < v.length; i++) {
                        Integer[] l = v[i];
                        if (i == idx) continue;
                        if (res.get()) break;
                        for (int j = 0; j < l.length; j++) {
                            if (!decision.equals(l[j])) continue;
                            l[j] = Integer.MIN_VALUE;
                            break;
                        }
                        if (Arrays.stream(l).noneMatch(ll -> ll != Integer.MIN_VALUE)) res.set(true);
                    }
                    return v;
                });
                return res.get();
            }
        };
    }

    // Implementation of decrease global constraint
    static PropagatorFunction decrease() {
        return new PropagatorFunction() {
            @Override
            public Boolean apply(Map<String, Object> params, String var, Integer idx, Integer decision,
                                 Map<String, Integer[][]> domains) {
                AtomicBoolean res = new AtomicBoolean(false);
                domains.computeIfPresent(var, (k, v) -> {
                    for (int i = 0; i < v.length; i++) {
                        Integer[] l = v[i];
                        if (i == idx) continue;
                        if (res.get()) break;
                        for (int j = l.length - 1; j >= 0; j--) {
                            if (l[j] == Integer.MIN_VALUE) continue;
                            if ((i < idx) ? (l[j] < decision) : (l[j] > decision))
                                l[j] = Integer.MIN_VALUE;
                        }
                        if (Arrays.stream(l).noneMatch(ll -> ll != Integer.MIN_VALUE)) res.set(true);
                    }
                    return v;
                });
                return res.get();
            }
        };
    }

    static PropagatorFunction hi() {
        return new PropagatorFunction() {
            @Override
            public Boolean apply(Map<String, Object> params, String var, Integer idx, Integer decision,
                                 Map<String, Integer[][]> domains) {
                for (Map.Entry<String, Integer[][]> a : domains.entrySet()) {
                    if (a.getKey().equals(var)) continue;
                    Integer[] aa = a.getValue()[idx];
                    for (int i = 0; i < aa.length; i++)
                        if (aa[i].equals(decision)) aa[i] = Integer.MIN_VALUE;
                    if (Arrays.stream(aa).noneMatch(i -> i != Integer.MIN_VALUE)) return true;
                }
                return false;
            }
        };
    }

    static PropagatorFunction hi2() {
        return new PropagatorFunction() {
            @Override
            public Boolean apply(Map<String, Object> params, String var, Integer idx, Integer decision,
                                 Map<String, Integer[][]> domains) {
                Integer row = Integer.parseInt(var.split("_")[1]);
                Integer n = (Integer) params.get("N");
                Integer s = (int) Math.sqrt(n);
                Integer i1 = row / s;
                int j1 = idx / s;
                for (Map.Entry<String, Integer[][]> a : domains.entrySet()) {
                    Integer row2 = Integer.parseInt(a.getKey().split("_")[1]);
                    Integer i2 = row2 / s;
                    if (a.getKey().equals(var) || !i2.equals(i1)) continue;
                    for (int j = j1 * s; j < (j1 + 1) * s; j++) {
                        Integer[] aa = a.getValue()[j];
                        for (int k = 0; k < aa.length; k++)
                            if (aa[k].equals(decision)) aa[k] = Integer.MIN_VALUE;
                        if (Arrays.stream(aa).noneMatch(i -> i != Integer.MIN_VALUE)) return true;
                    }
                }
                return false;
            }
        };
    }
}
