package uk.ac.ed.inf;

/**
 * The Node object to use
 * if two nodes are 'very close' to each other (distance < 0.00003 degree), they equals
 */
public class Node implements Comparable<Node>{
    public final LongLat loc;
    public final double g;
    public final double f;
    public final int angleFromParent;

    public final Node parent;

    public Node(LongLat loc, double g, double h, int angle, Node parent) {
        this.g = g;
        this.loc = loc;
        // f has direct euclidean distance as heuristic and
        f = g * LongLat.distance + h;
        angleFromParent = angle;
        this.parent = parent;
    }

    @Override
    public boolean equals(Object o){
        if (o == this) return true;
        if (!(o instanceof Node)) return false;
        return loc.veryCloseTo(((Node) o).loc);
    }


    @Override
    public int compareTo(Node other) {
        return Double.compare(this.f, other.f);
    }
}
