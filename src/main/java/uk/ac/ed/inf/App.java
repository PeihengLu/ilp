package uk.ac.ed.inf;

import com.mapbox.geojson.*;
import com.mapbox.geojson.Polygon;

import java.util.*;

/**
 * The lunch delivery drone control system
 */
public class App 
{
    /** the locations of points of interest */
    private static final HashMap<String, LongLat> locations = new HashMap<>();
    /** names of all locations of interests, used for composing and indexing the
     * graph to search on */
    private static final List<String> locationNames = new ArrayList<>();
    /** Order objects representing the orders on that day, ordered by delivery cost*/
    private static final Queue<Order> orders = new PriorityQueue<>(Collections.reverseOrder());
    /** the graph for the pathfinding algorithm to search on, it will store the shortest
     * distance between two nodes after running the shortest path algorithm*/
    private static Integer[][] graph;
    /** storing the vertices in shortest path algorithm for us to reconstruct the path */
    private static Integer[][] next;
    /** no-fly zones */
    private static final List<Polygon> noFlyZones = new ArrayList<Polygon>();
    /** the name of the locations that the drone needs to follow for current order */
    private static final Queue<String> path = new LinkedList<>();
    /** the name of locations the drone needs to follow for the current lag */
    private static Queue<String> currPath = new LinkedList<>();
    /** store the points the drone has taken and write it to the output geojson file */
    private static List<Point> pathRec = new ArrayList<>();
    /** create a menus object for calculating delivery cost and information about the shops */
    private static Menus menus;

    /** GeoJsonUtils, DatabaseUtils and W3WUtils services */
    private static GeoJsonUtils geoJsonUtils;
    private static DatabaseUtils databaseUtils;
    private static W3WUtils wUtils;

    private static Drone drone;

    public static void main( String[] args )
    {

        FeatureCollection nfzRedraw;

        // first add the starting and ending point Appleton Tower into our
        // points of interests
        locations.put("Appleton Tower", LongLat.AT);
        locationNames.add("Appleton Tower");


        if (args.length != 5) {
            System.err.println("You should enter 5 arguments: day, month, year, webserver port and database port!");
        }
        // fetch the commandline arguments and store them in variables
        String day = args[0];
        String month = args[1];
        String year = args[2];
        String port = args[3];
        String dbPort = args[4];
        // date of the orders to retrieve
        String date = String.join("-", year, month, day);
        // name of the server for connection
        String name = "localhost";
        // name of the output geojson file storing the path
        String outputFile = "../" + String.join("-", "drone", day, month, year) + ".geojson";

        // connect menus to the server
        menus = new Menus(name, port);
        // start the GeoJsonUtils, DatabaseUtils and W3WUtils services
        geoJsonUtils = new GeoJsonUtils(name, port);
        databaseUtils = new DatabaseUtils(name, dbPort, "derbyDB");
        wUtils = new W3WUtils(name, port);
        // initialize the Drone at appleton tower
        drone = new Drone(LongLat.AT, "Appleton Tower");


        // get the no-fly zones
        List<Feature> nfz = GeoJsonUtils.readGeoJson(geoJsonUtils.noFlyZone);
        if (nfz == null) {
            System.err.println("Problem reading noflyzone geojson file");
            return;
        }
        for (Feature f: nfz) {
            if (f.geometry() instanceof Polygon) {
                noFlyZones.add((Polygon) f.geometry());
            }
        }
        System.out.println("We have " + noFlyZones.size() + " no-fly zones");


        // get the landmarks
        List<Feature> landmarks = GeoJsonUtils.readGeoJson(geoJsonUtils.landmarks);
        if (landmarks == null) {
            System.err.println("Problem reading GeoJson file landmarks");
            return;
        }
        for (Feature landmark: landmarks) {
            Location loc = wUtils.convertW3W(landmark.getStringProperty("location"));
            if (loc == null) {
                System.err.println("Problem reading W3W address file");
                return;
            }
            locations.put(landmark.getStringProperty("name"), new LongLat(loc.coordinates));
            locationNames.add(landmark.getStringProperty("name"));
        }


        // get all the shops
        Collection<Shop> allShops = menus.provider.values();
        for (Shop shop: allShops) {
            Location loc = wUtils.convertW3W(shop.getLocation());
            if (loc == null) {
                System.err.println("Problem reading W3W address file");
                return;
            }
            if (!locations.containsKey(shop.getName())) {
                locations.put(shop.getName(), new LongLat(loc.coordinates));
                locationNames.add(shop.getName());
            }
        }


        // get the order details
        List<String[]> orderNoDeliver = databaseUtils.getOrders(date);
        if (orderNoDeliver == null) {
            System.err.println("Problem reading orders table");
            return;
        }
        if (!getOrders(orderNoDeliver)) return;

        int S = locationNames.size();

        System.out.println(locationNames);

        graph = new Integer[S][S];
        next = new Integer[S][S];

        populateGraph();

        System.out.println(Arrays.deepToString(graph));

        shortestPath();

        System.out.println(Arrays.deepToString(graph));

        for (Order order: orders) {
            System.out.println(order.orderNo);
        }
        // go through all the orders and plan the path for each
        while (!orders.isEmpty()) {
            path.clear();

            Order curr = orders.poll();
            // the delivery address of the current order
            LongLat deliverAddress = new LongLat(curr.deliverTo.coordinates);
            // name of the current location of the drone
            String start = drone.getCurrLocName();
            // name of the delivery address
            String end = curr.deliverTo.words;
            if (curr.shops.size() == 2) {
                String shopAName = curr.shops.get(0).getName();
                String shopBName = curr.shops.get(1).getName();

                if (getDistance(start, shopAName) + (getDistance(end, shopBName)) >
                        getDistance(start, shopBName) + (getDistance(end, shopAName))) {
                    // 0 is the index of Appleton Tower in locationNames, make sure the drone can still return after the order
                    // if not, check the next order
                    if (getDistance(start, shopBName) + getDistance(end, shopAName) + getDistance(shopAName, shopBName)
                            + getDistance(end, "Appleton Tower") > drone.getMoves()) {
                        continue;
                    }
                    path.add(shopBName);
                    path.add(shopAName);
                } else {
                    if (getDistance(start, shopAName) + getDistance(end, shopBName) + getDistance(shopAName, shopBName)
                            + getDistance(end, "Appleton Tower") > drone.getMoves()) {
                        continue;
                    }
                    path.add(shopAName);
                    path.add(shopBName);
                }
                path.add(end);
            } else {
                String shopName = curr.shops.get(0).getName();
                if (getDistance(start, shopName) + getDistance(shopName, end)
                        + getDistance(end, "Appleton Tower") > drone.getMoves()) {
                    continue;
                }
                path.add(shopName);
                path.add(end);
            }

            // if the program gets here, then the order basically will be carried out,
            // so store it to our orders database
            databaseUtils.storeOrder(curr.orderNo, curr.deliverTo.words, curr.deliveryCost);

            while (!path.isEmpty()) {
                String keypoint = path.poll();
                findPath(drone.getCurrLocName(), keypoint);
                // follow the path of one lag of the journey to reach a keypoint in current order
                while (!currPath.isEmpty()) {
                    String targetLoc = currPath.poll();
                    drone.setTargetLoc(locations.get(targetLoc), targetLoc);

                    if (!followOneLag(curr.orderNo)) return;
                }
            }

            drone.hover();
            drone.planNextMove();
            drone.makeNextMove();
        }


        // fly back to Appleton tower
        findPath(drone.getCurrLocName(), "Appleton Tower");
        // follow the path of one lag of the journey to reach a keypoint in current order
        while (!currPath.isEmpty()) {
            String targetLoc = currPath.poll();
            drone.setTargetLoc(locations.get(targetLoc), targetLoc);

            if (!followOneLag("NoOrder")) return;
        }

        LineString result = LineString.fromLngLats(pathRec);
        GeoJsonUtils.writeGeoJson(FeatureCollection.fromFeature(Feature.fromGeometry(result)), outputFile);
    }


    /**
     *
     * @return
     * @param orderNo
     */
    private static boolean followOneLag(String orderNo) {
        while (!drone.hasArrived()) {
            drone.calculateAngle();
            drone.planNextMove();

            if (checkNFZ(drone.getCurrLoc(), drone.getNextLoc())) {
                clockCounterclock(drone.getCurrLoc(), drone.getAngle());
                System.out.println("move fixed");
            }
//            System.out.println("(" + drone.getCurrLoc().longitude + ", " + drone.getCurrLoc().latitude +")");

            // store flight path into the database
            if (!databaseUtils.storePath(orderNo, drone.getCurrLoc(), drone.getAngle(), drone.getNextLoc())) {
                System.err.println("Problem writing to flightpath table");
                return false;
            }
            pathRec.add(Point.fromLngLat(drone.getCurrLoc().longitude, drone.getCurrLoc().latitude));
            drone.makeNextMove();
        }
        return true;
    }


    /**
     * get out of the no-fly zone using the clockwise counterclockwise maneuver explained in the document
     * @param currLoc
     * @param angle
     * @return
     */
    private static void clockCounterclock(LongLat currLoc, int angle) {
        int turn = 1;
        if (drone.getIntercept() != 0) {
            if (drone.getIntercept() == 1) {
                while (true) {
                    if (!checkNFZ(currLoc, currLoc.nextPosition(angle + turn * 10))) {
                        drone.setNextLoc(currLoc.nextPosition(angle + turn * 10));
                        break;
                    }
                    turn ++;
                }
            } else {
                while (true) {
                    if (!checkNFZ(currLoc, currLoc.nextPosition(angle - turn * 10))) {
                        drone.setNextLoc(currLoc.nextPosition(angle - turn * 10));
                        break;
                    }
                    turn ++;
                }
            }
        }
        while (true) {
            if (!checkNFZ(currLoc, currLoc.nextPosition(angle + turn * 10))) {
                drone.setIntercept(1);
                drone.setNextLoc(currLoc.nextPosition(angle + turn * 10));
                break;
            }
            if (!checkNFZ(currLoc, currLoc.nextPosition(angle - turn * 10))) {
                drone.setIntercept(-1);
                drone.setNextLoc(currLoc.nextPosition(angle - turn * 10));
                break;
            }
            turn++;
        }
    }


    /**
     *
     * @param orderNoDeliver the List of String arrays containing order number and delivery address
     * @return true no error occurs, false otherwise
     */
    private static boolean getOrders(List<String[]> orderNoDeliver) {
        for (String[] order : orderNoDeliver) {
            String orderNo = order[0];
            System.out.println(orderNo);
            // add all deliver destinations to the locations map as well
            // I simply named all the destinations of orders with their
            // corresponding w3w address
            String w3w = order[1];
            Location deliverTo = wUtils.convertW3W(w3w);
            if (deliverTo == null) {
                System.err.println("Problem reading W3W address file");
                return false;
            }
            if (!locations.containsKey(w3w)) {
                locations.put(w3w, new LongLat(deliverTo.coordinates));
                locationNames.add(w3w);
            }

            List<String> items = databaseUtils.getItems(orderNo);
            if (items == null) {
                System.err.println("Problem reading the orderDetails table");
                return false;
            }

            int deliveryCost = menus.getDeliveryCost(items.toArray(new String[0]));
            if (deliveryCost == -1) {
                System.err.println("Problem reading the menus file or illegal order");
                return false;
            }

            List<Shop> shops = new ArrayList<Shop>(menus.getShopped());
            // create an Order object containing the information just acquired
            // and add it to the list of orders on the specified date
            Order newOrder = new Order(orderNo, w3w, items, deliverTo, shops, deliveryCost);
            orders.add(newOrder);
        }
        return true;
    }


    /**
     * check if a line represented by p1 and p2 intercept with any edges of no-fly zones, also p2 cannot escape
     * our confinement area
     * @param p1 starting point
     * @param p2 target location
     * @return false if it doesn't go into the no-fly zones or confinement area, true otherwise
     */
    private static boolean checkNFZ(LongLat p1, LongLat p2) {
        if (!p2.isConfined()) return true;
        Point point1 = Point.fromLngLat(p1.longitude, p1.latitude);
        Point point2 = Point.fromLngLat(p2.longitude, p1.latitude);

        for (Polygon polygon: noFlyZones) {
            if (GeoJsonUtils.pathInterceptPolygon(point1, point2, polygon)) return true;
        }

        return false;
    }


    private static void populateGraph() {
        for (int i = 0; i < locationNames.size(); i ++) {
            for (int j = i + 1; j < locationNames.size(); j ++) {
                String locA = locationNames.get(i);
                String locB = locationNames.get(j);
                int weight = (int) Math.ceil(locations.get(locA).distanceTo(locations.get(locB)) * 1.01 / 0.00015) + 1;
                if (checkNFZ(locations.get(locA), locations.get(locB))) weight = weight * 10;
                graph[i][j] = weight;
                graph[j][i] = weight;
            }
        }
    }


    /**
     * Floyd-Warshall all pairs shortest path algorithm
     */
    private static void shortestPath() {
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
     * recover the path from next
     * @param locA
     * @param locB
     */
    private static void findPath(String locA, String locB) {
        // make sure no element is left
        currPath.clear();

        int indA = locationNames.indexOf(locA);
        int indB = locationNames.indexOf(locB);

        if (next[indA][indB] == null) {
            return;
        }

        while (indA != indB) {
            indA = next[indA][indB];
            currPath.add(locationNames.get(indA));
        }

        currPath.add(locationNames.get(indB));
    }

    private static int getDistance(String locA, String locB) {
        return graph[locationNames.indexOf(locA)][locationNames.indexOf(locB)];
    }
}
