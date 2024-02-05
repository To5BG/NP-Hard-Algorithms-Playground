package solver;

import java.util.BitSet;
import java.util.Map;

public class ConstraintPropagators {
    public static PentaFunction<Map<String, Object>, String, Integer, Integer, Map<String, BitSet[]>, Boolean>
    get(String option, int arg) {
        switch (option) {
            case "alldiff":
                return alldiff(arg);
            case "decrease":
                return decrease();
            default:
                return null;
        }
    }

    // Implementation of alldiff global constraint; arg -> what value is allowed to repeat
    static PentaFunction<Map<String, Object>, String, Integer, Integer, Map<String, BitSet[]>, Boolean>
    alldiff(int arg) {
        return (params, var, idx, decision, domains) -> {
            // Allow for exception values
            if (decision.equals(arg)) return false;
            BitSet[] v = domains.get(var);
            // All-diff -> unset decision bit for all vars different from current one
            for (int i = 0; i < v.length; i++) {
                if (i == idx) continue;
                v[i].clear(decision);
                // Empty domain -> unsatisfiable
                if (v[i].isEmpty()) return true;
            }
            return false;
        };
    }

    // Implementation of decrease global constraint
    static PentaFunction<Map<String, Object>, String, Integer, Integer, Map<String, BitSet[]>, Boolean>
    decrease() {
        return (params, var, idx, decision, domains) -> {
            BitSet[] v = domains.get(var);
            BitSet mask = new BitSet(v[0].size());
            // Decrease -> unset all values below decision, non-inclusive, for variables before idx
            for (int mi = 0; mi < decision; mi++) mask.set(mi);
            for (int i = 0; i < idx; i++) {
                v[i].andNot(mask);
                // Empty domain -> unsatisfiable
                if (v[i].isEmpty()) return true;
            }
            // Non-inclusive below
            mask.set(decision);
            // And all values above decision, for variables above idx
            for (int i = idx + 1; i < v.length; i++) {
                v[i].and(mask);
                // Empty domain -> unsatisfiable
                if (v[i].isEmpty()) return true;
            }
            return false;
        };
    }
}