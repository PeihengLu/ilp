package uk.ac.ed.inf;

/**
 * The Node object used by A* algorithm
 * if two nodes are 'very close' to each other, they equals
 */
public class Node implements Comparable<Node>{
    /** the long lat coordinate of the current Node */
    public final LongLat loc;
    /** the moves taken to reach current location */
    public final double g;
    /** summation of moved taken as well as heuristic */
    public final double f;
    /** the angle to take to go from parent to child */
    public final int angleFromParent;
    /** the parent Node of current Node, null if it's the root */
    public final Node parent;

    /**
     * initialize a Node object given necessary information
     * @param loc the coordinate of current location
     * @param g the moves take to reach current location
     * @param h the calculated heuristic
     * @param angle the angle to take to travel from parent Node to current Node
     * @param parent the parent Node of current Node
     */
    public Node(LongLat loc, double g, double h, int angle, Node parent) {
        this.g = g;
        this.loc = loc;
        // f has direct euclidean distance as heuristic and
        f = g * LongLat.distance + h;
        angleFromParent = angle;
        this.parent = parent;
    }

    /**
     * Two nodes are considered equal if they are 'very close to' each other
     * @param o the other Node to compare to
     * @return true if the two Nodes are considered equal, false otherwise
     */
    @Override
    public boolean equals(Object o){
        if (o == this) return true;
        if (!(o instanceof Node)) return false;
        return loc.veryCloseTo(((Node) o).loc);
    }

    /**
     * A Node is considered better than the other if the f value is smaller
     * @param other the other Node to compare with
     * @return true if current f is smaller than the f of other Node
     */
    @Override
    public int compareTo(Node other) {
        return Double.compare(this.f, other.f);
    }
}
