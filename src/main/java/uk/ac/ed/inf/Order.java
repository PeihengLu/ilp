package uk.ac.ed.inf;

import java.util.List;

/**
 * Order object that contains necessary order information, and can be compared with each
 * other with the deliveryCost
 */
public class Order implements Comparable<Order> {
    /** order number */
    public final String orderNo;
    /** the name of the order delivery address, simply its corresponding w3w word */
    public final String locationName;
    /** the items contained in the order */
    public final List<String> items;
    /** the coordinate of the delivery address */
    public final LongLat deliverAddress;
    /** the list of the shops that needs to be visited for an order */
    public final List<Shop> shops;
    /** how much the user needs to pay for this order */
    public final int deliveryCost;


    /**
     * initialize the order object
     * @param orderNo the order number
     * @param locationName the name of delivery address of the order (the w3w representation of the address)
     * @param items the items contained by the order
     * @param deliverAddress the coordinate of the delivery address
     * @param shops the list of the shops that needs to be visited for an order
     * @param deliveryCost how much the user needs to pay for this order
     */
    public Order(String orderNo, String locationName, List<String> items, LongLat deliverAddress, List<Shop> shops, int deliveryCost) {
        this.orderNo = orderNo;
        this.locationName = locationName;
        this.items = items;
        this.deliverAddress = deliverAddress;
        this.shops = shops;
        this.deliveryCost = deliveryCost;
    }

    /**
     * compare the orders by delivery cost only
     * @param o the other order to compare to
     * @return the difference between the delivery cost of this and other order
     */
    @Override
    public int compareTo(Order o) {
        return deliveryCost - o.deliveryCost;
    }
}
