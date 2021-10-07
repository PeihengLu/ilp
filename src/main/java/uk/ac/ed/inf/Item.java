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
     * construct an Item object storing information about food item
     * @param item item name
     * @param pence item price in pence
     */
    public Item(String item, Integer pence) {
        this.item = item;
        this.pence = pence;
    }

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
