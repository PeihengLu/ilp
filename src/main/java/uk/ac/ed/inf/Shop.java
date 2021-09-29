package uk.ac.ed.inf;

import java.util.ArrayList;


/**
 * class storing name, location and items for sale inside a shop
 */
public class Shop {
    /** name of the shop */
    private String name;
    /** location of the shop in W3W address format */
    private String location;
    /** list of items for sale in the shop */
    private ArrayList<Item> menu;


    /**
     * get the name of the shop
     * @return name of the shop
     */
    public String getName() {
        return name;
    }


    /**
     * get the location of the shop
     * @return location of the shop in W3W format
     */
    public String getLocation() {
        return location;
    }


    /**
     * get the items for sale in the shop
     * @return the list of Item representing the items for shop in the list
     */
    public ArrayList<Item> getMenu() {
        return menu;
    }
}
