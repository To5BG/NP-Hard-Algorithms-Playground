import search.*;

import java.util.*;
import java.util.stream.Collectors;

// A* search

public class AStar<S, A> {
    public static <S, A> Solution<S, A> search(HeuristicProblem<S, A> prob) {
        /// Your implementation goes here.
        Map<S, Double> dist = new HashMap<>();
        Queue<Node<S, A>> queue = new PriorityQueue<>();
        S start = prob.initialState();

        dist.put(start, 0.0);
        queue.add(new Node<>(start, 0, prob.estimate(start)));
        Node<S, A> curr = null;

        while (!queue.isEmpty()) {
            curr = queue.poll();
            if (prob.isGoal(curr.state)) break;
            for (A action : prob.actions(curr.state)) {
                // Extract new state from current action
                S next = prob.result(curr.state, action);
                // Calculate new cost
                Double newCost = curr.g + prob.cost(curr.state, action);
                // If new cost is larger than previous, skip iteration
                if (newCost + curr.h >= dist.getOrDefault(next, Double.MAX_VALUE)) continue;
                // Update dist entry
                dist.put(next, newCost);
                // Initialize new state
                Node<S, A> newState = new Node<>(next, newCost, prob.estimate(next));
                // Set parent and its action for backtracking
                newState.parent = curr;
                newState.parentAction = action;
                queue.add(newState);
            }
        }

        if (curr == null) return null;
        List<A> actions = new ArrayList<>();
        double pathCost = curr.g;
        S goalState = curr.state;

        while (curr.state != start) {
            actions.add(curr.parentAction);
            curr = curr.parent;
        }
        Collections.reverse(actions);
        // Print path
        // System.out.println(actions.stream().map(Object::toString).collect(Collectors.joining()));
        return new Solution<>(actions, goalState, pathCost);
    }


    public static class Node<S, A> implements Comparable<Node<S, A>> {

        public S state;
        public Node<S, A> parent;
        public A parentAction;
        public Double g;
        public Double h;

        public Node(S state, double g, double h) {
            this.state = state;
            this.g = g;
            this.h = h;
        }

        @Override
        public int compareTo(Node<S, A> o) {
            return Double.compare(this.g + this.h, o.g + o.h);
        }
    }
}
