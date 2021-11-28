package uk.ac.ed.inf;

import com.mapbox.geojson.*;

import java.util.*;

public class Drone {
    /** the current location of the drone */
    private LongLat currLoc;
    private String currLocName;
    /** angle to reach the next location */
    private int angle;
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
     * construct the Drone object with where the Drone is deployed from
     * @param currLoc the starting location of the drone
     */
    public Drone(LongLat currLoc, String locName, DatabaseUtils databaseUtils) {
        this.currLoc = currLoc;
        this.currLocName = locName;
        this.arrived = false;
        this.moves = 1500;
        this.databaseUtils = databaseUtils;
    }


    /** initialize the three arrays storing the graph information and run the all pairs shortest path algorithm
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
            waypoints.add(map.locationNames.get(indA));
            curr = indA;
        }

        return 0;
    }


    /**
     * check the availability of the current most profitable order, and deliver it if it is,
     * otherwise check the next order until the orders are all checked
     * @param orders The list of orders to deliver on a specified date
     * @return true if the drone manages to go through the orders and return to Appleton Tower,
     *          false otherwise
     */
    public boolean deliverOrders(Queue<Order> orders) {
        int totalOrders = orders.size();
        int orderSent = 0;
        int totalEarned = 0;
        int totalCost = 0;
        // go through all the orders and plan the path for each
        while (!orders.isEmpty()) {
            Order currOrder = orders.poll();
            totalCost += currOrder.deliveryCost;
            // check if an order is available and plan its path if it is
            if (!checkAvailability(currOrder)) {
                System.out.printf("Apologies, cannot finish order %s, not enough power left, lost %d pence\n", currOrder.orderNo, currOrder.deliveryCost);
                continue;
            }

            // if the program gets here, then the order will be carried out,
            // so store it to our orders database
            databaseUtils.storeOrder(currOrder.orderNo, currOrder.locationName, currOrder.deliveryCost);

            if (!followPathForOrder(currOrder)) {
                System.err.printf("Failed to complete order %s due to planning error\n", currOrder.orderNo);
                return false;
            }

            orderSent ++;
            totalEarned += currOrder.deliveryCost;
        }

        if (!returnToAppleton()) {
            System.err.println("Error when returning to Appleton tower");
        }

        System.out.printf("Out of %d orders, %d orders were sent, made %d pence, %.3f money made, and returned to AT? %b\n", totalOrders, orderSent, totalEarned, (double) totalEarned / (double) totalCost, currLoc.closeTo(LongLat.AT));
        return true;

    }


    /**
     * Check if the drone has enough power to finish the order or if finishing current order
     * will leave enough battery for the drone to return to Appleton Tower, add the shops, delivery address
     * for the order to path if the drone has enough power. It also compares the shortest path if two shops
     * need to be visited.
     * @param currOrder the order number of the order whose availability needs to be checked
     * @return true if the drone can finish the order and has enough power to return to AT,
     *          false otherwise
     */
    private boolean checkAvailability(Order currOrder) {
        // name of the current location of the drone
        String start = currLocName;
        // name of the delivery address
        String end = currOrder.locationName;
        // the number of times the drone needs to hover
        int hover = 0;
        // if an order requires visiting two shops, make a quick comparison as well as checking the availability
        if (currOrder.shops.size() == 2) {
            // the drone needs to hover over the two shops and the delivery address
            hover = 3;
            String shopAName = currOrder.shops.get(0).getName();
            String shopBName = currOrder.shops.get(1).getName();

            if (map.getDistance(start, shopAName) + (map.getDistance(end, shopBName)) >
                    map.getDistance(start, shopBName) + (map.getDistance(end, shopAName))) {
                if (map.getDistance(start, shopBName) + map.getDistance(end, shopAName) + map.getDistance(shopAName, shopBName)
                        + map.getDistance(end, "Appleton Tower") + hover > moves) {
                    return false;
                }
                path.add(shopBName);
                path.add(shopAName);
            } else {
                if (map.getDistance(start, shopAName) + map.getDistance(end, shopBName) + map.getDistance(shopAName, shopBName)
                        + map.getDistance(end, "Appleton Tower") + hover > moves) {
                    return false;
                }
                path.add(shopAName);
                path.add(shopBName);
            }
            path.add(end);
        } else {
            // the drone needs to hover over the shop and the delivery address
            hover = 2;
            // only one shop for this order, no comparison needed, just check availability
            String shopName = currOrder.shops.get(0).getName();
            if (map.getDistance(start, shopName) + map.getDistance(shopName, end)
                    + map.getDistance(end, "Appleton Tower") + hover > moves) {
                return false;
            }
            path.add(shopName);
            path.add(end);
        }
        return true;
    }

    /**
     * follow the pre-planned path for an order
     * @param currOrder the order number of the order currently taken
     * @return true if no errors occurred on the path
     */
    private boolean followPathForOrder(Order currOrder) {
        while (!path.isEmpty()) {
            String keypoint = path.poll();
            int intersected = findPath(currLocName, keypoint);
            if (intersected == -1) {
                System.err.println("Problem retrieving the path");
            }
            // if any part of the path intersect with no-fly zones, use A star instead
            if (intersected == 1) {
                Stack<Integer> plannedPath = planAStar(getLocations().get(keypoint));
                moveAStar(currOrder.orderNo, plannedPath);
            }
            if (!followWaypoints(currOrder.orderNo)) return false;

            // after reaching every keypoint hover for one step
            hover();
            planNextMove();
            makeNextMove(currOrder.orderNo);
        }
        return true;
    }

    private boolean followWaypoints(String orderNo) {
        // follow the path of one lag of the journey to reach a keypoint in current order
        while (!waypoints.isEmpty()) {
            String targetLoc = waypoints.poll();
            setTargetLoc(map.locations.get(targetLoc), targetLoc);

            if (!followOneLag(orderNo)) {
                return false;
            }
        }
        return true;
    }

    /**
     * following the path to one of the waypoints, a lag is the trip between two waypoints
     * @param orderNo the order number of the current order the drone is working on
     * @return false if error occurs when following the preplanned path or storing the path to database
     */
    private boolean followOneLag(String orderNo) {
        while (!arrived) {
            calculateAngle();
            if (moves == 0) {
                System.err.println("Drone stuck");
                return false;
            }
            planNextMove();
            makeNextMove(orderNo);
        }
        return true;
    }

    /**
     * return the drone to Appleton tower
     * @return true if the drone made it back to the Tower, false otherwise
     */
    private boolean returnToAppleton() {
        // fly back to Appleton tower
        findPath(currLocName, "Appleton Tower");
        // follow the path to reach Appleton
        return followWaypoints("NoOrder");
    }

    //--------------------------- drone planning and movement in normal situation --------------------------------//

    /**
     * calculate the optimal angle to reach the desired location
     */
    public void calculateAngle() {
        if (targetLoc == null) {
            System.err.println("The target of the drone is not yet set");
        }

        double ang = Math.toDegrees(Math.atan2(targetLoc.lat - currLoc.lat, targetLoc.lng - currLoc.lng));
        if (ang < 0) ang += 360;
        angle = roundToTen(ang);
    }

    /**
     * round the optimal degree calculated to 10 degree precision
     * @param toRound
     * @return
     */
    public static int roundToTen(double toRound) {
        return (int) (Math.round(toRound / 10) * 10);
    }

    /**
     * get the next location to go to after calculating the angle
     */
    public void planNextMove() {
        if (angle % 10 != 0 && angle != -999) {
            System.err.println("An invalid move");
        }
        nextLoc = currLoc.nextPosition(angle);
        if (map.intersectNFZ(currLoc, nextLoc)) {
            clockCounterclock();
            // recalculate the next location with the new angle
            planNextMove();
        }
    }

    /**
     * set the target location of the drone for one lag
     * @param targetLoc
     */
    public void setTargetLoc(LongLat targetLoc, String targetLocName) {
        this.targetLoc = targetLoc;
        this.targetLocName = targetLocName;
        // a new lag has started so arrived is refreshed to false
        this.arrived = false;
    }

    /**
     * call this instead of getAngle when the drone has reached a destination
     */
    public void hover() {
        this.angle = -999;
    }

    /**
     * change currPos to nextPos
     */
    public void makeNextMove(String orderNo) {
        addToPathRec(currLoc);
        // store flight path into the database
        if (!databaseUtils.storePath(orderNo, currLoc, angle, nextLoc)) {
            System.err.println("Problem writing to flightpath table");
        }
        currLoc = nextLoc;
        if (currLoc.closeTo(targetLoc)) {
            arrived = true;
            currLocName = targetLocName;
        }
        moves --;
    }

    //---------------------- drone planning when needed to avoid obstacle -------------------------------//

    /**
     * get out of the no-fly zone using the clockwise counterclockwise maneuver explained in the document
     * use preAngle to keep the momentum and help avoid getting stuck
     */
    private void clockCounterclock() {
        System.out.println("needs to avoid NFZ");
        int turn = 1;
        while (true) {
            if (!map.intersectNFZ(currLoc, currLoc.nextPosition(angle + turn * 10))) {
                this.angle = angle + turn * 10;
                planNextMove();
                break;
            }
            if (!map.intersectNFZ(currLoc, currLoc.nextPosition(angle - turn * 10))) {
                this.angle = angle - turn * 10;
                planNextMove();
                break;
            }
            turn++;
        }
    }



    /**
     * plan the path with AStar
     * @param target the LongLat coordinate of the location to reach
     * @return true if a path is viable with current accuracy, false otherwise
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
                // if the next location is outside confinement area or the path there intersect
                // with no-fly zone, it's not added to the lists
                if (map.intersectNFZ(best.loc, next) || !next.isConfined()) continue;
                Node child = new Node(next, best.g + 1, next.distanceTo(target), i, best);
                // if a node has never been visited before
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
     * @param orderNo the order number of the order being carried out
     * @param pathPlanned the list of angles planned by A*
     * @return true if the path can be followed successfully
     */
    public boolean moveAStar(String orderNo, Stack<Integer> pathPlanned) {
        // follow the path planned by A star
        while (!pathPlanned.isEmpty()) {
            if (moves == 0) return false;
            int anglePlaned = pathPlanned.pop();
            this.angle = anglePlaned;
            planNextMove();
            // double check to make sure the route is legal
            if (map.intersectNFZ(currLoc, nextLoc)) {
                System.err.println("Path planned by A* intersects with no-fly zone");
            }
            makeNextMove(orderNo);
        }

        return true;
    }


    //---------------------------------- populating map with information -------------------------------------/
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

    /**
     * add to the recorded path to write into the geojson file
     * @param longLat
     */
    public void addToPathRec(LongLat longLat) {
        Point point = Point.fromLngLat(longLat.lng, longLat.lat);
        pathRec.add(point);
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
     * get next location in the path for current order
     * @return the next location in the path for current order
     */
    public List<Point> getPathRecord() {
        return pathRec;
    }
}
