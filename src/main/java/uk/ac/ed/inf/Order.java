package uk.ac.ed.inf;

import java.util.List;

/**
 * Order object that contains necessary order information, and can be compared with each
 * other with a metric, currently only the deliveryCost
 */
public class Order implements Comparable<Order> {
    public final String orderNo;
    /** the name of the order delivery point, simply its corresponding w3w word */
    public final String locationName;
    public final List<String> items;
    public final Location deliverTo;
    public final List<Shop> shops;
    public final int deliveryCost;


    /**
     * initialize the order object
     * @param orderNo the order number
     * @param locationName the name of delivery address of the order (the w3w representation of the address)
     * @param items the items contained by the order
     * @param deliverTo the coordinate of the delivery address
     * @param shops the list of the shops that needs to be visited for an order
     * @param deliveryCost how much the user needs to pay for this order
     */
    public Order(String orderNo, String locationName, List<String> items, Location deliverTo, List<Shop> shops, int deliveryCost) {
        this.orderNo = orderNo;
        this.locationName = locationName;
        this.items = items;
        this.deliverTo = deliverTo;
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
