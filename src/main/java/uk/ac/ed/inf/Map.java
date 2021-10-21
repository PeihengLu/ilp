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
    /** Order objects representing the orders on that day, ordered by delivery cost*/
    public final Queue<Order> orders = new PriorityQueue<>(Collections.reverseOrder());
    /** the graph for the pathfinding algorithm to search on, it will store the shortest
     * distance between two nodes after running the shortest path algorithm*/
    public Integer[][] graph;
    /** storing the vertices in shortest path algorithm for us to reconstruct the path */
    public Integer[][] next;
    /** no-fly zones */
    public final List<Polygon> noFlyZones = new ArrayList<Polygon>();


    /**
     * make a weighted graph of all points of interest, with the weight being
     * the estimated number of steps needed, the weight will be multiplied by 2
     * if the path intersect with no-fly zones, suggesting that this path is not
     * recommended but still can be taken if no other ways work
     */
    public void populateGraph() {
        for (int i = 0; i < locationNames.size(); i ++) {
            for (int j = i + 1; j < locationNames.size(); j ++) {
                String locA = locationNames.get(i);
                String locB = locationNames.get(j);
                int weight = (int) Math.ceil(locations.get(locA).distanceTo(locations.get(locB)) * 1.01 / 0.00015) + 1;
                if (checkNFZ(locations.get(locA), locations.get(locB))) weight = weight * 2;
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

        for (int i = 0; i < s; i ++) {
            for (int j = 0; j < s; j++) {
                next[i][j] = j;
            }
        }

        for (int i = 0; i < s; i ++) {
            next[i][i] = i;
            graph[i][i] = 0;
        }

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
    public boolean checkNFZ(LongLat p1, LongLat p2) {
        if (!p2.isConfined()) return true;
        Point point1 = Point.fromLngLat(p1.longitude, p1.latitude);
        Point point2 = Point.fromLngLat(p2.longitude, p2.latitude);

        for (Polygon polygon: noFlyZones) {
            if (GeoJsonUtils.pathInterceptPolygon(point1, point2, polygon)) return true;
        }

        return false;
    }


    public int getDistance(String locA, String locB) {
        return graph[locationNames.indexOf(locA)][locationNames.indexOf(locB)];
    }

}
