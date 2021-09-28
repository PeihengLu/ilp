package uk.ac.ed.inf;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;


/**
 * Reads the menus' information from restaurants of interest from
 * designated web server and provide function to calculate the
 * delivery cost of an order, as well as if an order is legal
 */
public class Menus {
    // the JSONArray read from the menus json file on the web server
    private final JSONArray json;

    /**
     * initializing by connecting to the server and read the content
     * of the JSON file into a JSONArray
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
        this.json = rj.readJsonFromUrl();
    }


    /**
     * check if an order is within our delivery policy and calculate
     * the total price if it's legal
     * @param items items from an order
     * @return -1 if item number or shop composition is against our
     * policy, returns delivery cost otherwise
     */
    public int getDeliveryCost(String... items) {
        // an order can only have [1, 4] items
        if (items.length < 1 || items.length > 4) return -1;
        // maximum amount of shops an order's good can come from
        int maxShop = 2;
        // 50p of delivery charges
        int val = 50;
        ArrayList<String> itemList = new ArrayList<>(Arrays.asList(items));
        // todo: this search needs to be optimised
        for (int i = 0; i < json.length(); i ++) {
            // change to true if at least one item is from the current shop
            boolean shopped = false;
            // have visited the maximum amount of shops but still cannot find
            // all the goods needed, the order will be deemed invalid
            if (maxShop == 0 && !itemList.isEmpty()) return -1;
            JSONObject shop = json.getJSONObject(i);
            JSONArray menu = shop.getJSONArray("menu");
            // go through the list of items in the current shop and look for desired items
            for (int j = 0; j < menu.length(); j ++) {
                JSONObject item = menu.getJSONObject(j);
                if (itemList.contains(item.get("item"))) {
                    val += (int) item.get("pence");
                    itemList.remove(item.get("item"));
                    shopped = true;
                }
                // if all shopping have been found return the prices
                if (itemList.isEmpty()) return val;
            }
            if (shopped) maxShop --;
        }
        // if not all items can be found
        if (!itemList.isEmpty()) return -1;
        return val;
    }
}
