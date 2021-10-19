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
    private static HashMap<String, LongLat> locations = new HashMap<>();
    /** names of all locations of interests, used for composing and indexing the
     * graph to search on */
    private static List<String> locationNames = new ArrayList<>();
    // Order objects representing the orders on that day
    private static List<Order> orders = new ArrayList<>();
    // the graph for the pathfinding algorithm
    private static Integer[][] graph;
    // no-fly zones
    private static List<Polygon> noFlyZones = new ArrayList<Polygon>();

    private static Menus menus;

    /** GeoJsonUtils, DatabaseUtils and W3WUtils services */
    private static GeoJsonUtils geoJsonUtils;
    private static DatabaseUtils databaseUtils;
    private static W3WUtils wUtils;

    public static void main( String[] args )
    {
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


        // create a menus object for calculating delivery cost and information about the shops
        menus = new Menus(name, port);
        // start the GeoJsonUtils, DatabaseUtils and W3WUtils services
        geoJsonUtils = new GeoJsonUtils(name, port);
        databaseUtils = new DatabaseUtils(name, dbPort, "derbyDB");
        wUtils = new W3WUtils(name, port);


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

        // sort the orders by the delivery cost
        Collections.sort(orders);

        System.out.println("We have locations: " + Arrays.toString(locationNames.toArray()));


//        LineString result;
//        Feature line = Feature.fromGeometry(result);
//        FeatureCollection path = FeatureCollection.fromFeature(line);
//        GeoJsonUtils.writeGeoJson(FeatureCollection.fromFeatures(path), outputFile);

    }


    /**
     *
     * @param orderNoDeliver the List of String arrays containing order number and delivery address
     * @return true no error occurs, false otherwise
     */
    private static boolean getOrders(List<String[]> orderNoDeliver) {
        for (String[] order : orderNoDeliver) {
            String orderNo = order[0];
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
     * check if a line represented by p1 and p2 intercept with any edges of no-fly zones
     * @param p1 starting point
     * @param p2 target location
     * @return false if it doesn't go into the no-fly zones, true otherwise
     */
    private boolean checkNFZ(LongLat p1, LongLat p2) {
        Point point1 = Point.fromLngLat(p1.longitude, p1.latitude);
        Point point2 = Point.fromLngLat(p2.longitude, p1.latitude);
        boolean intercept = false;

        for (Polygon polygon: noFlyZones) {
            intercept = intercept || GeoJsonUtils.pathInterceptPolygon(point1, point2, polygon);
        }

        return intercept;
    }
}
