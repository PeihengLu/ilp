package uk.ac.ed.inf;

import java.util.ArrayList;


/**
 * class storing name, location and items for sale of a shop
 */
public class Shop {
    /** name of the shop */
    public final String name;
    /** location of the shop in W3W address format */
    public final String location;
    /** list of items for sale in the shop, set to private as final doesn't offer protection for the content of ArrayList */
    private final ArrayList<Item> menu;


    /**
     * construct a Shop object storing information about a shop
     * @param name name of the shop
     * @param location location of the shop in W3W address format
     * @param menu list of items for sale in the shop
     */
    public Shop(String name, String location, ArrayList<Item> menu) {
        this.name = name;
        this.location = location;
        this.menu = menu;
    }

    /**
     * get the items for sale in the shop
     * @return the list of Item representing the items for shop in the list
     */
    public ArrayList<Item> getMenu() {
        return menu;
    }
}
