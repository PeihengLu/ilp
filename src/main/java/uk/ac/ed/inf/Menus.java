package uk.ac.ed.inf;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.*;


/**
 * Reads the menus' information from restaurants of interest from
 * designated web server and provide function to calculate the
 * delivery cost of an order, as well as if an order is legal
 */
public class Menus {
    /** record the shop where a food item is sold (itemName, shop) */
    public final HashMap<String, Shop> provider = new HashMap<>();
    /** record the prices of food items (itemName, price) */
    public final HashMap<String, Integer> prices = new HashMap<>();
    /** record the shops that's visited for one order */
    private final HashSet<Shop> shopped = new HashSet<>();


    /**
     * initializing by connecting to the server and read the content
     * of the JSON file and store them into HashMaps
     * storing each item's price and provider information
     * @param name the machine name of the online server
     * @param port the port of connection to the online server
     */
    public Menus(String name, String port) {
        // url of the server location
        String server = "HTTP://" + name + ":" + port;
        // location of the menus file on the server
        String AddressToMenu = server + "/menus/menus.json";
        // start HTTP connection with the server
        ServerUtils connection = new ServerUtils(AddressToMenu);

        // the file content read from the menus json file on the web server
        String shopsJSON = connection.readStringFromUrl();
        if (shopsJSON == null) {
            System.err.println("Trouble connecting to server");
            return;
        }

        // parse the menu with Gson into a list of Shop instances
        ArrayList<Shop> shops = getShops(shopsJSON);
        if (shops == null) return;

        // populate provider and prices Hash tables with information from the server
        getItemInfo(shops);
    }

    /**
     * parse the information from the server into a list of shops
     * @param menus
     * @return
     */
    private ArrayList<Shop> getShops(String menus) {
        Type shopListType = new TypeToken<ArrayList<Shop>>() {}.getType();
        ArrayList<Shop> shops = new Gson().fromJson(menus, shopListType);
        if (shops == null) {
            System.err.println("Issue with parsing of JSON file");
            return null;
        }
        return shops;
    }


    /**
     * get the useful information about an item: which shop is it sold in,
     * and for what price. Then stores them in the HashMaps providers and prices
     * @param shops the Shop instances parsed from the server
     */
    private void getItemInfo(ArrayList<Shop> shops) {
        for (Shop shop: shops) {
            ArrayList<Item> menu = shop.getMenu();
            for (Item i : menu) {
                provider.put(i.getName(), shop);
                prices.put(i.getName(), i.getPrice());
            }
        }
    }


    /**
     * check if an order complies with our delivery policy and calculate
     * the total price if it's legal
     * @param items items from an order
     * @return -1 if item number or shop composition is against our
     * policy or not all food can be found, returns delivery cost otherwise
     */
    public int getDeliveryCost(String... items) {
        // an order can only have [1, 4] items
        if (items.length < 1 || items.length > 4) return -1;
        // record the name of shops visited for one order
        shopped.clear();
        // 50p of delivery charges
        int val = 50;

        ArrayList<String> itemList = new ArrayList<>(Arrays.asList(items));

        for (String item: itemList) {
            // if item cannot be found in any shop, return error value -1
            if (!provider.containsKey(item)) return -1;
            shopped.add(provider.get(item));
            val += prices.get(item);
        }
        // if items from one order is composed of food from more than two shops, it's
        // against our delivery policy
        if (shopped.size() > 2) {
            System.err.println("an order shouldn't have items from more than 2 shops");
            return -1;
        }
        return val;
    }

    /**
     * get the shops to visit for an order after calculating its delivery cost
     * @return
     */
    public HashSet<Shop> getShopped() {
        return shopped;
    }
}
