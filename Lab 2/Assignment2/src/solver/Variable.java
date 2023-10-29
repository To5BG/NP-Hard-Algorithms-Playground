package solver;

import java.util.List;

// Variable wrapper
public class Variable {

    // domain of possible values
    List<Integer> domain;
    // array of possible values
    Integer[] value;

    public Variable(List<Integer> domain, Integer[] value) {
        this.domain = domain;
        this.value = value;
    }
}
