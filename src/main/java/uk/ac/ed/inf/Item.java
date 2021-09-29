package uk.ac.ed.inf;


/**
 * class storing name and price (in pence) of food item
 */
public class Item {
    /** item name */
    private String item;
    /** item price in pence*/
    private Integer pence;

    /**
     * get the name of the item
     * @return name of the item
     */
    public String getName() {
        return item;
    }


    /**
     * get the price of the item
     * @return price of the item in pence
     */
    public Integer getPrice() {
        return pence;
    }
}
