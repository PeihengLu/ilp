package uk.ac.ed.inf;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

import java.util.*;

public class Drone {
    /** the current location of the drone */
    private LongLat currLoc;
    private String currLocName;
    /** angle to reach the next location */
    private int angle;
    /** angle used last time */
    private int anglePre;
    /** where to go next*/
    private LongLat nextLoc;
    /** the target of the current lag of journey */
    private LongLat targetLoc;
    private String targetLocName;
    /** whether the drone is close to target location */
    private boolean arrived;
    /** how many moves the drone got left */
    private int moves;
    /** the name of the locations that the drone needs to follow for current order */
    private final Queue<String> path = new LinkedList<>();
    /** the waypoints (all existing locations) the drone needs to follow for a target location */
    private final Queue<String> waypoints = new LinkedList<>();
    /** store the points the drone has taken and write it to the output geojson file */
    private final List<Point> pathRec = new ArrayList<>();
    /** to record the path for AStarMove */
    DatabaseUtils databaseUtils;
    /** map object storing useful locations for orders on the specified date */
    private final Map map = new Map();



    /**
     * construct the Drone object with where the Drone is
     * @param currLoc
     */
    public Drone(LongLat currLoc, String locName, DatabaseUtils databaseUtils) {
        this.currLoc = currLoc;
        this.currLocName = locName;
        this.arrived = false;
        this.moves = 1500;
        this.databaseUtils = databaseUtils;
    }


    /**
     * plan the path with AStar
     * @param target
     * @return
     */
    public Stack<Integer> planAStar(LongLat target) {
        LongLat cur = currLoc;
        Node start = new Node(cur, 0, cur.distanceTo(target), 0, null);
        // contains the node we have encountered but haven't analysed yet
        Queue<Node> open = new PriorityQueue<>();
        // contains node whose children are all added to the list or disqualified
        List<Node> closed = new ArrayList<>();
        // the best path to target given by the algorithm
        Node solution = null;

        open.add(start);

        while (!open.isEmpty()) {
            Node best = open.peek();
            if (best.loc.closeTo(target)) {
                solution = best;
                break;
            }

            // drone can only move in angles with 10 degree of precision
            for (int i = 0; i <= 350; i += 10) {
                LongLat next = best.loc.nextPosition(i);
                if (map.intersectNFZ(best.loc, next) || !next.isConfined()) continue;
                Node child = new Node(next, best.g + 1, next.distanceTo(target), i, best);
                if (!open.contains(child) && !closed.contains(child)) {
                    open.add(child);
                } else {
                    if (closed.contains(child)) {
                        Node childSame = closed.get(closed.indexOf(child));
                        if (childSame.f > child.f) {
                            closed.remove(childSame);
                            open.add(child);
                        }
                    }
                }
            }

            open.remove(best);
            closed.add(best);
        }

        if (solution == null) {
            System.err.println("A star didn't find a solution");
            return null;
        }

        Stack<Integer> aStar = new Stack<>();

        while (solution.parent != null) {
            aStar.push(solution.angleFromParent);
            solution = solution.parent;
        }

        return aStar;
    }

    /**
     * Follow the path planned by
     * @param orderNo
     * @param pathPlanned
     * @return
     */
    public boolean moveAStar(String orderNo, Stack<Integer> pathPlanned) {
        // follow the path planned by A star
        while (!pathPlanned.isEmpty()) {
            if (moves == 0) return false;
            int anglePlaned = pathPlanned.pop();
            setNextLoc(currLoc.nextPosition(anglePlaned));
            databaseUtils.storePath(orderNo, currLoc, anglePlaned, nextLoc);
            makeNextMove();
            addToPathRec(currLoc);
        }

        return true;
    }



    /**
     * set the target location of the drone
     * @param targetLoc
     */
    public void setTargetLoc(LongLat targetLoc, String targetLocName) {
        this.targetLoc = targetLoc;
        this.targetLocName = targetLocName;
        // a new lag has started so these fields are refreshed as they are storing
        // information only about one lag
        this.arrived = false;
    }

    /**
     * manually set the nextLoc when automatically generated one is illegal
     * @param nextLoc the specified location for the drone to move to next
     */
    public void setNextLoc(LongLat nextLoc) {
        this.nextLoc = nextLoc;
    }

    /**
     * call this instead of getAngle when the drone has reached a destination
     */
    public void hover() {
        this.angle = -999;
    }


    /**
     * set nextPos with the target location
     */
    public void calculateAngle() {
        if (targetLoc == null) {
            System.err.println("The target of the drone is not yet set");
        }

        double ang = Math.toDegrees(Math.atan2(targetLoc.latitude - currLoc.latitude, targetLoc.longitude - currLoc.longitude));
        if (ang < 0) ang += 360;
        angle = roundToTen(ang);
    }


    public static int roundToTen(double toRound) {
        return (int) (Math.round(toRound / 10) * 10);
    }

    public void planNextMove() {
        nextLoc = currLoc.nextPosition(angle);
    }

    /**
     * add no-fly zones to the map the drone is storing
     */
    public void addNFZ(Polygon nfz) {
        map.noFlyZones.add(nfz);
    }

    /**
     *
     * @param name name of the location
     * @param loc LongLat object storing the coordinate of the locations
     */
    public void addLocation(String name, LongLat loc) {
        map.locations.put(name, loc);
        map.locationNames.add(name);
    }

    /** initialize the three arrays storing the graph information
     * @param size the size of the graph, same as number of locations in the map
     */
    public void initializeGraph(int size) {
        map.graph = new Integer[size][size];
        map.next = new Integer[size][size];
        map.intersect = new Boolean[size][size];
        map.populateGraph();
        map.shortestPath();
    }


    /**
     * change currPos to nextPos
     */
    public void makeNextMove() {
        currLoc = nextLoc;
        if (currLoc.closeTo(targetLoc)) {
            arrived = true;
            currLocName = targetLocName;
            anglePre = angle;
        }
        moves --;
    }

    /**
     * add the name of a location to visit for an order
     * @param name the name of the location
     */
    public void addToPath(String name) {
        path.add(name);
    }

    /**
     * add the name of the waypoint needed to reach a location
     * @param name the name of the waypoint
     */
    public void addToWaypoints(String name) {
        waypoints.add(name);
    }

    /**
     * add to the recorded path to write into the geojson file
     * @param longLat
     */
    public void addToPathRec(LongLat longLat) {
        Point point = Point.fromLngLat(longLat.longitude, longLat.latitude);
        pathRec.add(point);
    }

    /**
     * check if all the locations needed to visit for an order has been visited
     * @return true if all locations for current order has been visited
     */
    public boolean isPathEmpty () {
        return path.isEmpty();
    }

    /**
     * check if all the waypoints has been visited
     * @return true if waypoints is empty
     */
    public boolean isWaypointsEmpty() {
        return waypoints.isEmpty();
    }

    /**
     * return the locations hashmap in the map object
     * @return locations
     */
    public HashMap<String, LongLat> getLocations() {
        return map.locations;
    }

    /**
     * return the locationsNames list in the map object
     * @return locationNames
     */
    public List<String> getLocationNames() {
        return map.locationNames;
    }

    /**
     * get the distance between two locations
     * @param locA name of one location
     * @param locB name of the other location
     * @return
     */
    public int getDistance(String locA, String locB) {
        return map.getDistance(locA, locB);
    }

    /**
     * check if the direct path between two points intersect with no-fly zones
     * @param locA coordinate of one location
     * @param locB coordinate of the other location
     * @return true if the direct path between them intersect with no-fly zones
     */
    public boolean checkNFZ(LongLat locA, LongLat locB) {
        return map.intersectNFZ(locA, locB);
    }

    /**
     *
     * @param locA
     * @param locB
     * @return -1 if error occurs, 0 if no errors and no intersect, 1 if any part of the retrieved path intersect with
     *          the no-fly zones
     */
    public int findPath(String locA, String locB) {
        int indA = map.locationNames.indexOf(locA);
        int indB = map.locationNames.indexOf(locB);
        int curr = indA;

        if (map.next[indA][indB] == null) {
            return -1;
        }

        // test if any part of the path intersect with no-fly zones, if intersects, clear waypoints previously recorded
        while (indA != indB) {
            indA = map.next[indA][indB];
            if (map.intersect[curr][indA]) {
                waypoints.clear();
                return 1;
            }
            this.addToWaypoints(map.locationNames.get(indA));
            curr = indA;
        }

        return 0;
    }

    /**
     * get next location in the path for current order
     * @return the next location in the path for current order
     */
    public String getNextLocForOrder() {
        return path.poll();
    }

    public String getNextWaypointForLag() {
        return waypoints.poll();
    }

    public List<Point> getPathRecord() {
        return pathRec;
    }

    /** return how many moves the drone got left */
    public int getMoves() {
        return moves;
    }

    /**
     * get the current location of the drone
     * @return LongLat object storing the current location of the drone
     */
    public LongLat getCurrLoc() {
        return currLoc;
    }

    /**
     * get the planned location to head to, used to check intersection with no-fly zones
     * @return the planned location for the drone to move to in the next move
     */
    public LongLat getNextLoc() {
        return nextLoc;
    }

    /**
     * get the angle automatically planned by the drone
     * @return the angle planned by the drone
     */
    public int getAngle() {
        return angle;
    }

    /**
     * check if the drone has arrived at the target location
     * @return true if the drone is close to the target location
     */
    public boolean hasArrived() {
        return arrived;
    }

    /**
     * get the name of current location
     * @return
     */
    public String getCurrLocName() {
        return currLocName;
    }
}
