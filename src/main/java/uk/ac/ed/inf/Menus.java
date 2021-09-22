package uk.ac.ed.inf;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

public class Menus {
    private JSONArray json;

    public Menus(String name, String port) {
        String server = "HTTP://" + name + ":" + port;
        String menu = server + "/menus/menus.json";
        this.json = ReadJson.readJsonFromUrl(menu);
    }

    public int getDeliveryCost(String... items) {
        if (items.length < 1 || items.length > 4) return -1;
        ArrayList<String> itemList = new ArrayList<>();
        // 50p of delivery charges
        int val = 50;
        itemList.addAll(Arrays.asList(items));
        // todo: this search needs to be optimised
        for (int i = 0; i < json.length(); i ++) {
            if (itemList.isEmpty()) return val;
            JSONObject shop = json.getJSONObject(i);
            JSONArray menu = shop.getJSONArray("menu");
            for (int j = 0; j < menu.length(); j ++) {
                JSONObject item = menu.getJSONObject(j);
                if (itemList.contains(item.get("item"))) {
                    val += (int) item.get("pence");
                    itemList.remove(item.get("item"));
                }
            }
        }
        if (!itemList.isEmpty()) return -1;
        return val;
    }
}
