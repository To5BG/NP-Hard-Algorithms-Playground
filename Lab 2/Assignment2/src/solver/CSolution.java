package solver;

import java.util.ArrayList;
import java.util.List;

public class CSolution<T> {
    public List<T> solutions;
    public Integer count;

    public CSolution() {
        this.solutions = new ArrayList<>();
        this.count = 0;
    }
}