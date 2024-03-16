package solver;

import java.util.List;

// Variable wrapper
public class Variable {
    String name;
    List<Integer> domain; // Domain of possible values, prepended with size of domain (num of entries != MIN_VALUE)
    Integer[] val; // Array of possible values

    public Variable(String name, List<Integer> domain, Integer[] value) {
        this.name = name;
        this.domain = domain;
        this.val = value;
    }
}