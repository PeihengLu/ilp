package uk.ac.ed.inf;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

import java.util.*;

/**
 * The representation of the map with all necessary information for the program
 */
public class Map {
    /** the locations of points of interest */
    public final HashMap<String, LongLat> locations = new HashMap<>();
    /** names of all locations of interests, used for composing and indexing the
     * graph to search on */
    public final List<String> locationNames = new ArrayList<>();
    /** the graph for the pathfinding algorithm to search on, it will store the shortest
     * distance between two nodes after running the shortest path algorithm*/
    public Integer[][] graph;
    /** storing the vertices in shortest path algorithm for us to reconstruct the path */
    public Integer[][] next;

    public Boolean[][] intersect;
    /** no-fly zones */
    public final List<Polygon> noFlyZones = new ArrayList<Polygon>();

    /**
     * initialize the map object by adding the first known point of interest Appleton Tower
     */
    public Map() {
        // first add the starting and ending point Appleton Tower into our
        // points of interests
        this.locations.put("Appleton Tower", LongLat.AT);
        this.locationNames.add("Appleton Tower");
    }


    /**
     * make a weighted graph of all points of interest, with the weight being
     * the estimated number of steps needed, the weight will be multiplied by 2
     * if the path intersect with no-fly zones, suggesting that this path is not
     * recommended but still can be taken if no other ways work
     */
    public void populateGraph() {
        for (int i = 0; i < locationNames.size(); i ++) {
            for (int j = i; j < locationNames.size(); j ++) {
                // initialize distance to itself to 0, and the node to reach itself is itself
                if (i == j) {
                    graph[i][j] = 0;
                    next[i][j] = i;
                    intersect[i][j] = false;
                }

                // assign the estimated moves needed to travel between two nodes as weights
                String locA = locationNames.get(i);
                String locB = locationNames.get(j);
                int weight = (int) Math.ceil(locations.get(locA).distanceTo(locations.get(locB)) * 1.01 / 0.00015);
                if (intersectNFZ(locations.get(locA), locations.get(locB))) {
                    // if the direct path between two nodes intersect with no-fly zone, it should take no more than
                    // three times as much moves for A* to reach the destination
                    weight = weight * 3;
                    intersect[i][j] = true;
                } else {
                    intersect[i][j] = false;
                }
                intersect[j][i] = intersect[i][j];
                graph[i][j] = weight;
                graph[j][i] = weight;
            }
        }
    }


    /**
     * Floyd-Warshall all pairs shortest path algorithm
     */
    public void shortestPath() {
        int s = locationNames.size();

        // initializes the paths between two nodes to the direct path between them
        for (int i = 0; i < s; i ++) {
            for (int j = 0; j < s; j++) {
                next[i][j] = j;
            }
        }

        // adding more and more nodes into the partial graph and check if the
        // new node gives a shorter path between two nodes
        for (int l = 0; l < s; l ++) {
            for (int i = 0; i < s; i ++) {
                for (int j = 0; j < s; j ++) {
                    if (graph[i][j] > graph[i][l] + graph[l][j]) {
                        graph[i][j] = graph[i][l] + graph[l][j];
                        next[i][j] = next[i][l];
                    }
                }
            }
        }
    }

    /**
     * check if a line represented by p1 and p2 intercept with any edges of no-fly zones, also p2 cannot escape
     * our confinement area
     * @param p1 starting point
     * @param p2 target location
     * @return false if it doesn't go into the no-fly zones or confinement area, true otherwise
     */
    public boolean intersectNFZ(LongLat p1, LongLat p2) {
        if (!p2.isConfined()) return true;
        Point point1 = Point.fromLngLat(p1.lng, p1.lat);
        Point point2 = Point.fromLngLat(p2.lng, p2.lat);

        for (Polygon polygon: noFlyZones) {
            if (pathInterceptPolygon(point1, point2, polygon)) return true;
        }

        return false;
    }

    /**
     * effectively check if a line passes through the outer perimeter of a Polygon
     * @param p1 one end of the path (line segment) to check
     * @param p2 another end of the path to check
     * @param polygon the polygon representing one of the no-fly zones
     * @return true if the path intercept with the outer perimeter of the no-fly zone
     */
    public static boolean pathInterceptPolygon(Point p1, Point p2, Polygon polygon) {
        List<Point> lines = polygon.coordinates().get(0);
        // check the interception of the path with all line segments of the perimeter
        for (int i = 0; i < lines.size() - 1; i ++) {
            if (checkIntercept(p1, p2, lines.get(i), lines.get((i + 1)))) return true;
        }
        return false;
    }


    /**
     * check if two lines intercept with each other, ingenious solution from Bryce Boe:
     * https://bryceboe.com/2006/10/23/line-segment-intersection-algorithm/
     * using the counterclockwise helper function to deal with interception problems, cannot
     * deal with collinearity but suffice for our use
     * @param A one endpoint of line 1
     * @param B another endpoint of line 1
     * @param C one endpoint of line 2
     * @param D another endpoint of line 2
     * @return true if line 1 intercept with line 2
     */
    private static boolean checkIntercept(Point A, Point B, Point C, Point D) {
        return counterClockWise(A, C, D) != counterClockWise(B, C, D) &&
                counterClockWise(A, B, C) != counterClockWise(A, B, D);
    }


    /**
     * determines if three points A, B, C are listed in a counterclockwise order
     * @param A Point A
     * @param B Point B
     * @param C Point C
     * @return true if A, B, C are listed in a counterclockwise order
     */
    private static boolean counterClockWise(Point A, Point B, Point C) {
        double Ax = A.coordinates().get(0);
        double Ay = A.coordinates().get(1);
        double Bx = B.coordinates().get(0);
        double By = B.coordinates().get(1);
        double Cx = C.coordinates().get(0);
        double Cy = C.coordinates().get(1);
        // if slope of AB is less than the slope of AC, then A, B, C are listed in a counterclockwise orientation
        // use this form to avoid problems of divide by 0 when encounters vertical lines
        return (Cy - Ay) * (Bx - Ax) > (By - Ay) * (Cx - Ax);
    }

    /**
     * get the number of moves needed by the drone to move between two locations
     * or a very pessimistic estimation of the moves needed if the path intersect with
     * no-fly zones
     * @param locA the name of one location
     * @param locB the name of the other location
     * @return the moves the drone need to travel between locA and locB
     */
    public int getDistance(String locA, String locB) {
        return graph[locationNames.indexOf(locA)][locationNames.indexOf(locB)];
    }
}
