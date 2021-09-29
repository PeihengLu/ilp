package uk.ac.ed.inf;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.*;


/**
 * Reads the menus' information from restaurants of interest from
 * designated web server and provide function to calculate the
 * delivery cost of an order, as well as if an order is legal
 */
public class Menus {
    /** record where the food is sold */
    private final HashMap<String, String> provider = new HashMap<>();
    /** record the prices of food */
    private final HashMap<String, Integer> prices = new HashMap<>();


    /**
     * initializing by connecting to the server and read the content
     * of the JSON file into a JSONArray, and store them into HashMaps
     * storing each item's price and provider information
     * @param name the machine name of the online server
     * @param port the port of connection to the online server
     */
    public Menus(String name, String port) {
        // url of the server
        String server = "HTTP://" + name + ":" + port;
        // location of the menus file on the server
        String AddressToMenu = server + "/menus/menus.json";
        // load the content into a JSONArray object
        ConnectServer connection = new ConnectServer(AddressToMenu);

        // the file content read from the menus json file on the web server
        String response = connection.readStringFromUrl();
        if (response == null) {
            System.err.println("Trouble connecting to server");
            return;
        }

        // parse the response with Gson into a list of shops
        Type shopListType = new TypeToken<ArrayList<Shop>>() {}.getType();
        ArrayList<Shop> shops = new Gson().fromJson(response, shopListType);
        if (shops == null) {
            System.err.println("Issue with parsing of JSON file");
            return;
        }
        for (Shop shop: shops) {
            String shopName = shop.getName();
            ArrayList<Item> menu = shop.getMenu();
            for (Item i : menu) {
                provider.put(i.getName(), shopName);
                prices.put(i.getName(), i.getPrice());
            }
        }
    }


    /**
     * check if an order is within our delivery policy and calculate
     * the total price if it's legal
     * @param items items from an order
     * @return -1 if item number or shop composition is against our
     * policy or not all food can be found, returns delivery cost otherwise
     */
    public int getDeliveryCost(String... items) {
        // record the number of shops visited for one order
        HashSet<String> shopped = new HashSet<>();
        // an order can only have [1, 4] items
        if (items.length < 1 || items.length > 4) return -1;
        // 50p of delivery charges
        int val = 50;
        ArrayList<String> itemList = new ArrayList<>(Arrays.asList(items));
        for (String item: itemList) {
            // item cannot be found in any shop
            if (!provider.containsKey(item)) return -1;
            shopped.add(provider.get(item));
            val += prices.get(item);
        }
        if (shopped.size() > 2) return -1;
        return val;
    }
}
