package solver;

import java.util.List;

// Variable wrapper
public class Variable {

    // Domain of possible values, prepended with size of domain (num of entries != MIN_VALUE)
    List<Integer> domain;

    // Array of possible values
    Integer[] value;

    public Variable(List<Integer> domain, Integer[] value) {
        this.domain = domain;
        this.value = value;
    }
}