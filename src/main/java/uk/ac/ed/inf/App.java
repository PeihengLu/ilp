package uk.ac.ed.inf;

import com.mapbox.geojson.*;
import com.mapbox.geojson.Polygon;

import java.util.*;

/**
 * The lunch delivery drone control system
 */
public class App 
{
    /** GeoJsonUtils, DatabaseUtils and W3WUtils services */
    private static GeoJsonUtils geoJsonUtils;
    private static DatabaseUtils databaseUtils;
    private static W3WUtils wUtils;

    /** create a menus object for calculating delivery cost and information about the shops */
    private static Menus menus;
    /** drone object to store drone's location and energy information, also the planned path for it to follow*/
    private static Drone drone;
    /** map object storing useful locations for orders on the specified date */
    private static Map map = new Map();
    private static final Queue<Order> orders = new PriorityQueue<>(Collections.reverseOrder());

    private static String outputFile;
    private static String outputFileFailed;

    public static void main( String[] args )
    {
        int totalOrders ;
        int orderSent = 0;
        int totalCost = 0;
        // first add the starting and ending point Appleton Tower into our
        // points of interests
        map.locations.put("Appleton Tower", LongLat.AT);
        map.locationNames.add("Appleton Tower");


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
        outputFile = "../" + String.join("-", "drone", day, month, year) + ".geojson";
        outputFileFailed = "../" + String.join("-", "drone", day, month, year) + "failed" + ".geojson";

        // connect menus to the server
        menus = new Menus(name, port);
        // start the GeoJsonUtils, DatabaseUtils and W3WUtils services
        geoJsonUtils = new GeoJsonUtils(name, port);
        databaseUtils = new DatabaseUtils(name, dbPort, "derbyDB");
        wUtils = new W3WUtils(name, port);
        // initialize the Drone at appleton tower
        drone = new Drone(LongLat.AT, "Appleton Tower");


        // get the no-fly zones
        if (!getNoFlyZones()) {
            System.err.println("Cannot get the information about no fly zones");;
            return;
        }
        // get all the landmarks and add them to locations known by the drone
        if (!getLandmarks()) {
            System.err.println("Cannot get the information about landmarks");;
            return;
        }
        // get the shop information and add them to locations known by the drone
        if (!getShopsInfo()) {
            System.err.println("Cannot get the information about shops");;
            return;
        }
        // retrieve all orders for a specific date
        if (!getOrders(date)) {
            System.err.printf("Error retrieving orders for %s", date);
            return;
        }

        int S = map.locationNames.size();
        map.graph = new Integer[S][S];
        map.next = new Integer[S][S];
        map.populateGraph();
        map.shortestPath();

        totalOrders = orders.size();
        // go through all the orders and plan the path for each
        while (!orders.isEmpty()) {
            Order currOrder = orders.poll();
            // check if an order is available and plan its path if it is
            if (checkAvailability(currOrder)) {
                System.out.printf("Apologies, cannot finish order %s, not enough power left, lost %d pence\n", currOrder.orderNo, currOrder.deliveryCost);
                continue;
            }

            // if the program gets here, then the order will be carried out,
            // so store it to our orders database
            databaseUtils.storeOrder(currOrder.orderNo, currOrder.deliverTo.words, currOrder.deliveryCost);
            System.out.println("Starting order " + currOrder.orderNo);

            if (followPathForOrder(currOrder)) {
                System.err.printf("Failed to complete order %s due to planning error\n", currOrder.orderNo);
                return;
            }

            drone.hover();
            drone.planNextMove();
            drone.makeNextMove();

            orderSent ++;
            totalCost += currOrder.deliveryCost;
        }


        if (returnToAppleton()) return;

        LineString result = LineString.fromLngLats(drone.getPathRecord());
        GeoJsonUtils.writeGeoJson(FeatureCollection.fromFeature(Feature.fromGeometry(result)), outputFile);
        System.out.printf("Out of %d orders, %d orders were sent, made %d pence, and returned to AT? %b\n", totalOrders, orderSent, totalCost, drone.getCurrLoc().closeTo(LongLat.AT));
    }

    private static boolean getNoFlyZones() {
        List<Feature> nfz = GeoJsonUtils.readGeoJson(geoJsonUtils.noFlyZone);
        if (nfz == null) {
            System.err.println("Problem reading noflyzone geojson file");
            return false;
        }
        for (Feature f: nfz) {
            if (f.geometry() instanceof Polygon) {
                map.noFlyZones.add((Polygon) f.geometry());
            }
        }
        return true;
    }

    private static boolean getLandmarks() {
        // get the landmarks
        List<Feature> landmarks = GeoJsonUtils.readGeoJson(geoJsonUtils.landmarks);
        if (landmarks == null) {
            System.err.println("Problem reading GeoJson file landmarks");
            return false;
        }
        for (Feature landmark: landmarks) {
            Location loc = wUtils.convertW3W(landmark.getStringProperty("location"));
            if (loc == null) {
                System.err.println("Problem reading W3W address file");
                return false;
            }
            map.locations.put(landmark.getStringProperty("name"), new LongLat(loc.coordinates));
            map.locationNames.add(landmark.getStringProperty("name"));
        }
        return true;
    }

    /**
     * get the information of all the shops providing food for the service
     * @return
     */
    private static boolean getShopsInfo() {
        // get all the shops
        Collection<Shop> allShops = menus.provider.values();
        for (Shop shop: allShops) {
            Location loc = wUtils.convertW3W(shop.getLocation());
            if (loc == null) {
                System.err.println("Problem reading W3W address file");
                return false;
            }
            if (!map.locations.containsKey(shop.getName())) {
                map.locations.put(shop.getName(), new LongLat(loc.coordinates));
                map.locationNames.add(shop.getName());
            }
        }
        return true;
    }

    /**
     * follow the pre-planned path for an order
     * @param currOrder the order number of the order currently taken
     * @return
     */
    private static boolean followPathForOrder(Order currOrder) {
        while (!drone.isPathEmpty()) {
            String keypoint = drone.getNextLocForOrder();
            System.out.println("Going to " + keypoint);
            findPath(drone.getCurrLocName(), keypoint);
            // follow the path of one lag of the journey to reach a keypoint in current order
            while (!drone.isCurrPathEmpty()) {
                String targetLoc = drone.getNextWaypointForLag();
                if (!targetLoc.equals(keypoint)) {
                    System.out.println("Taking a detour through " + targetLoc);
                }
                drone.setTargetLoc(map.locations.get(targetLoc), targetLoc);

                if (!followOneLag(currOrder.orderNo)) {
                    LineString resultFailed = LineString.fromLngLats(drone.getPathRecord());
                    GeoJsonUtils.writeGeoJson(FeatureCollection.fromFeature(Feature.fromGeometry(resultFailed)), outputFileFailed);
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * return the drone to Appleton tower
     * @return true if the drone made it back to the Tower, false otherwise
     */
    private static boolean returnToAppleton() {
        // fly back to Appleton tower
        findPath(drone.getCurrLocName(), "Appleton Tower");
        // follow the path to reach Appleton
        while (!drone.isCurrPathEmpty()) {
            String targetLoc = drone.getNextWaypointForLag();
            drone.setTargetLoc(map.locations.get(targetLoc), targetLoc);

            if (!followOneLag("NoOrder")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the drone has enough power to finish the order or if finishing current order
     * will leave enough battery for the drone to return to Appleton Tower, add the shops, delivery address
     * for the order to path if the drone has enough power
     * @param currOrder the order number of the order whose availability needs to be checked
     * @return
     */
    private static boolean checkAvailability(Order currOrder) {
        System.out.println("Evaluating order " + currOrder.orderNo);
        // name of the current location of the drone
        String start = drone.getCurrLocName();
        // name of the delivery address
        String end = currOrder.deliverTo.words;
        if (currOrder.shops.size() == 2) {
            String shopAName = currOrder.shops.get(0).getName();
            String shopBName = currOrder.shops.get(1).getName();

            if (map.getDistance(start, shopAName) + (map.getDistance(end, shopBName)) >
                    map.getDistance(start, shopBName) + (map.getDistance(end, shopAName))) {
                // 0 is the index of Appleton Tower in locationNames, make sure the drone can still return after the order
                // if not, check the next order
                if (map.getDistance(start, shopBName) + map.getDistance(end, shopAName) + map.getDistance(shopAName, shopBName)
                        + map.getDistance(end, "Appleton Tower") > drone.getMoves()) {
                    return true;
                }
                drone.addToPath(shopBName);
                drone.addToPath(shopAName);
            } else {
                if (map.getDistance(start, shopAName) + map.getDistance(end, shopBName) + map.getDistance(shopAName, shopBName)
                        + map.getDistance(end, "Appleton Tower") > drone.getMoves()) {
                    return true;
                }
                drone.addToPath(shopAName);
                drone.addToPath(shopBName);
            }
            drone.addToPath(end);
        } else {
            String shopName = currOrder.shops.get(0).getName();
            if (map.getDistance(start, shopName) + map.getDistance(shopName, end)
                    + map.getDistance(end, "Appleton Tower") > drone.getMoves()) {
                return true;
            }
            drone.addToPath(shopName);
            drone.addToPath(end);
        }
        return false;
    }


    /**
     * following the path to one of the way points to a point of interest (the shop or drop of point to reach)
     * @param orderNo the order number of the current order the drone is working on
     * @return false if error
     */
    private static boolean followOneLag(String orderNo) {
        while (!drone.hasArrived()) {
            drone.calculateAngle();
            drone.planNextMove();

            if (drone.getMoves() == 0) {
                System.err.println("Drone stuck");
                return false;
            }

            if (map.checkNFZ(drone.getCurrLoc(), drone.getNextLoc())) {
                clockCounterclock(drone.getCurrLoc(), drone.getAngle());
            }
            // store flight path into the database
            if (!databaseUtils.storePath(orderNo, drone.getCurrLoc(), drone.getAngle(), drone.getNextLoc())) {
                System.err.println("Problem writing to flightpath table");
                return false;
            }
            drone.addToPathRec(Point.fromLngLat(drone.getCurrLoc().longitude, drone.getCurrLoc().latitude));
            drone.makeNextMove();
        }
        return true;
    }


    /**
     * get out of the no-fly zone using the clockwise counterclockwise maneuver explained in the document
     * use preAngle to keep the momentum and help avoid getting stuck
     * @param currLoc the current location of the drone
     * @param angle the angle calculated automatically by the drone
     */
    private static void clockCounterclock(LongLat currLoc, int angle) {
        int turn = 1;
//        if (drone.getIntercept() != 0) {
//            double angleWithMomentum = (angle * 0.1 + drone.getAnglePre() * 0.9);
//            if (drone.getIntercept() == 1) {
//                while (true) {
//                    if (!checkNFZ(currLoc, currLoc.nextPosition(Drone.roundToTen( angleWithMomentum) + turn * 10))) {
//                        drone.setNextLoc(currLoc.nextPosition(Drone.roundToTen( angleWithMomentum) + turn * 10));
//                        break;
//                    }
//                    turn ++;
//                }
//            } else {
//                while (true) {
//                    if (!checkNFZ(currLoc, currLoc.nextPosition(Drone.roundToTen(angleWithMomentum) - turn * 10))) {
//                        drone.setNextLoc(currLoc.nextPosition(Drone.roundToTen(angleWithMomentum) - turn * 10));
//                        break;
//                    }
//                    turn ++;
//                }
//            }
//        } else {
            while (true) {
                if (!map.checkNFZ(currLoc, currLoc.nextPosition(angle + turn * 10))) {
                    drone.setIntercept(1);
                    drone.setNextLoc(currLoc.nextPosition(angle + turn * 10));
                    break;
                }
//                if (!checkNFZ(currLoc, currLoc.nextPosition(angle - turn * 10))) {
//                    drone.setIntercept(-1);
//                    drone.setNextLoc(currLoc.nextPosition(angle - turn * 10));
//                    break;
//                }
                turn++;
            }
//        }
    }


    /**
     *
     * @param date
     * @return true no error occurs, false otherwise
     */
    private static boolean getOrders(String date) {
        // get the order details
        List<String[]> orderNoDeliver = databaseUtils.getOrders(date);
        if (orderNoDeliver == null) {
            System.err.println("Problem reading orders table");
            return false;
        }
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
            if (!map.locations.containsKey(w3w)) {
                map.locations.put(w3w, new LongLat(deliverTo.coordinates));
                map.locationNames.add(w3w);
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

            List<Shop> shops = new ArrayList<>(menus.getShopped());
            // create an Order object containing the information just acquired
            // and add it to the list of orders on the specified date
            Order newOrder = new Order(orderNo, w3w, items, deliverTo, shops, deliveryCost);
            orders.add(newOrder);
        }
        return true;
    }


    /**
     * recover the path from next
     * @param locA
     * @param locB
     */
    private static void findPath(String locA, String locB) {
        int indA = map.locationNames.indexOf(locA);
        int indB = map.locationNames.indexOf(locB);

        if (map.next[indA][indB] == null) {
            return;
        }

        // todo: figure out what happened here
        while (indA != indB) {
            indA = map.next[indA][indB];
            drone.addToCurrPath(map.locationNames.get(indA));
        }
//
//        currPath.add(locationNames.get(indB));
    }
}
