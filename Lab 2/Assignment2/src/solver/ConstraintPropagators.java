package solver;

import java.util.BitSet;
import java.util.Map;

public class ConstraintPropagators {
    public static PentaFunction<Map<String, Object>, Integer, Integer, Integer, BitSet[][], Boolean> get(String option, int arg) {
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
    static PentaFunction<Map<String, Object>, Integer, Integer, Integer, BitSet[][], Boolean> alldiff(int arg) {
        return (params, var, idx, decision, domains) -> {
        return (_p, varIdx, _e, decision, domains) -> {
            // Allow for exception values
            if (decision.equals(arg)) return false;
            BitSet[] v = domains[varIdx];
            // All-diff -> unset decision bit for all vars different from current one
            for (BitSet bitSet : v) {
                if (bitSet == null) continue;
                bitSet.clear(decision);
                // Empty domain -> unsatisfiable
                if (bitSet.isEmpty()) return true;
            }
            return false;
        };
    }

    // Implementation of decrease global constraint
    static PentaFunction<Map<String, Object>, Integer, Integer, Integer, BitSet[][], Boolean> decrease() {
        return (params, var, idx, decision, domains) -> {
            BitSet[] v = domains[var];
        return (_p, varIdx, elIdx, decision, domains) -> {
            BitSet[] v = domains[varIdx];
            BitSet mask = null;
            for (BitSet bitSet : v) {
                if (bitSet == null) continue;
                mask = new BitSet(bitSet.size());
                break;
            }
            if (mask == null) {
                return false;
            }
            // Decrease -> unset all values below decision, non-inclusive, for variables before idx
            for (int mi = 0; mi < decision; mi++) mask.set(mi);
            for (int i = 0; i < elIdx; i++) {
                if (v[i] == null) continue;
                v[i].andNot(mask);
                // Empty domain -> unsatisfiable
                if (v[i].isEmpty()) return true;
            }
            // Non-inclusive below
            mask.set(decision);
            // And all values above decision, for variables above idx
            for (int i = elIdx + 1; i < v.length; i++) {
                if (v[i] == null) continue;
                v[i].and(mask);
                // Empty domain -> unsatisfiable
                if (v[i].isEmpty()) return true;
            }
            return false;
        };
    }
}