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
    private static final Queue<Order> orders = new PriorityQueue<>(Collections.reverseOrder());

    public static void main( String[] args)
    {
        // calculating the runtime of the system
        long startTime = System.nanoTime();
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
        // name of the output file if the software failed to produce a complete path
        String outputFileFailed = "../" + String.join("-", "drone", day, month, year) + "failed" + ".geojson";

        // connect menus to the server
        menus = new Menus(name, port);
        // start the GeoJsonUtils, DatabaseUtils and W3WUtils services
        geoJsonUtils = new GeoJsonUtils(name, port);
        databaseUtils = new DatabaseUtils(name, dbPort, "derbyDB");
        wUtils = new W3WUtils(name, port);

        // initialize the Drone at appleton tower with fully initialized map and dbUtil to
        // record the path for A star
        drone = new Drone(LongLat.AT, "Appleton Tower", databaseUtils);


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

        // after all locations are loaded into the map, initialize the three arrays representing the
        // graph to
        int s = drone.getLocationNames().size();
        drone.initializeGraph(s);


        if (!drone.deliverOrders(orders)){
            GeoJsonUtils.writeGeoJson(FeatureCollection.fromFeature(Feature.fromGeometry(LineString.fromLngLats(drone.getPathRecord()))), outputFileFailed);
            System.err.println("Failed to deliver the orders or return to Appleton");
            return;
        }

        LineString result = LineString.fromLngLats(drone.getPathRecord());
        GeoJsonUtils.writeGeoJson(FeatureCollection.fromFeature(Feature.fromGeometry(result)), outputFile);

        long endTime = System.nanoTime();
        double duration = (endTime - startTime) / 1000000000.0;
        System.out.printf("takes %.2f seconds\n", duration);
    }

    /**
     * retrieves the no-fly zones from the server and store them in the drone
     * @return true if no error occurs, false otherwise
     */
    private static boolean getNoFlyZones() {
        // retrieve no-fly zones from the server
        List<Feature> nfz = GeoJsonUtils.readGeoJson(geoJsonUtils.noFlyZone);
        if (nfz == null) {
            System.err.println("Problem reading noflyzone geojson file");
            return false;
        }
        // store the no-fly zones as a list of Polygons in the drone
        for (Feature f: nfz) {
            if (f.geometry() instanceof Polygon) {
                drone.addNFZ((Polygon) f.geometry());
            }
        }
        return true;
    }

    /**
     * Get the landmarks used to avoid no-fly zones, and add them to the map of the drone
     * @return true if no error occurs, false otherwise
     */
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
            drone.addLocation(landmark.getStringProperty("name"), loc.coordinates);
        }
        return true;
    }

    /**
     * get the information of all the shops providing food for the service, and add them
     * to the map of the drone if it's not already added
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
            if (!drone.getLocations().containsKey(shop.getName())) {
                drone.addLocation(shop.getName(), loc.coordinates);
            }
        }
        return true;
    }


    /**
     * Retrieve the orders for a specified date. Save them in the app class and
     * later pass it to the drone
     * @param date the date when the orders are made
     * @return true no error occurs, false otherwise
     */
    private static boolean getOrders(String date) {
        // get the order details
        List<String[]> orderNoDeliver = databaseUtils.retrieveOrders(date);
        if (orderNoDeliver == null) {
            System.err.println("Problem reading orders table");
            return false;
        }
        for (String[] order : orderNoDeliver) {
            String orderNo = order[0];
            // add all deliver destinations to the locations map as well
            // I simply named all the destinations of orders with their
            // corresponding w3w address
            String w3w = order[1];
            if (wUtils.convertW3W(w3w) == null) {
                System.err.println("Problem reading W3W address file");
                return false;
            }
            LongLat deliverTo = wUtils.convertW3W(w3w).coordinates;
            // add the delivery address to the map if it isn't already there
            if (!drone.getLocations().containsKey(w3w)) {
                drone.addLocation(w3w, deliverTo);
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
}
