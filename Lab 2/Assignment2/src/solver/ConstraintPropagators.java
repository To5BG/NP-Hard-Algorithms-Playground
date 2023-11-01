package solver;

import java.util.Arrays;
import java.util.Map;

public class ConstraintPropagators {

    public static PropagatorFunction get(String option, int arg) {
        switch (option) {
            case "alldiff":
                return alldiff(arg);
            case "decrease":
                return decrease();
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
                Integer[][] v = domains.get(var);
                for (int i = 0; i < v.length; i++) {
                    Integer[] l = v[i];
                    if (i == idx) continue;
                    for (int j = 0; j < l.length; j++) {
                        if (!decision.equals(l[j])) continue;
                        l[j] = Integer.MIN_VALUE;
                        break;
                    }
                    if (Arrays.stream(l).noneMatch(ll -> ll != Integer.MIN_VALUE)) return true;
                }
                return false;
            }
        };
    }

    // Implementation of decrease global constraint
    static PropagatorFunction decrease() {
        return new PropagatorFunction() {
            @Override
            public Boolean apply(Map<String, Object> params, String var, Integer idx, Integer decision,
                                 Map<String, Integer[][]> domains) {
                Integer[][] v = domains.get(var);
                for (int i = 0; i < v.length; i++) {
                    Integer[] l = v[i];
                    if (i == idx) continue;
                    for (int j = l.length - 1; j >= 0; j--) {
                        if (l[j] == Integer.MIN_VALUE) continue;
                        if ((i < idx) ? (l[j] < decision) : (l[j] > decision))
                            l[j] = Integer.MIN_VALUE;
                    }
                    if (Arrays.stream(l).noneMatch(ll -> ll != Integer.MIN_VALUE)) return true;
                }
                return false;
            }
        };
    }
}