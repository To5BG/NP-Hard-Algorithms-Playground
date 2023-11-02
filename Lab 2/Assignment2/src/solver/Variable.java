package solver;

import java.util.List;

// Variable wrapper
public class Variable {
    List<Integer> domain; // Domain of possible values, prepended with size of domain (num of entries != MIN_VALUE)
    Integer[] val; // Array of possible values

    public Variable(List<Integer> domain, Integer[] value) {
        this.domain = domain;
        this.val = value;
    }
}