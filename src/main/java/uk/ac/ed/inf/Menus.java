package uk.ac.ed.inf;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;


/**
 * Reads the menus' information from restaurants of interest from
 * designated web server and provide function to calculate the
 * delivery cost of an order, as well as if an order is legal
 */
public class Menus {
    // record where the food is sold
    private HashMap<String, String> provider = new HashMap<>();
    // record the prices of food
    private HashMap<String, Integer> prices = new HashMap<>();


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
        String menu = server + "/menus/menus.json";
        // load the content into a JSONArray object
        ReadJson rj = new ReadJson(menu);
        // the JSONArray read from the menus json file on the web server
        JSONArray json = rj.getJsonArray();
        // read the menus file and store them in the HashMaps
        for (int i = 0; i < json.length(); i ++) {
            JSONObject shop = json.getJSONObject(i);
            String shopName = shop.getString("name");
            JSONArray menus = shop.getJSONArray("menu");
            // go through the list of items in the current shop and look for desired items
            for (int j = 0; j < menus.length(); j++) {
                JSONObject item = menus.getJSONObject(j);
                provider.put(item.getString("item"), shopName);
                prices.put(item.getString("item"), (int) item.get("pence"));
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
