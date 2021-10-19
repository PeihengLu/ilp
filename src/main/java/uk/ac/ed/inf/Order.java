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
     *
     * @param orderNo
     * @param locationName
     * @param items
     * @param deliverTo
     * @param shops
     * @param deliveryCost
     */
    public Order(String orderNo, String locationName, List<String> items, Location deliverTo, List<Shop> shops, int deliveryCost) {
        this.orderNo = orderNo;
        this.locationName = locationName;
        this.items = items;
        this.deliverTo = deliverTo;
        this.shops = shops;
        this.deliveryCost = deliveryCost;
    }


    @Override
    public int compareTo(Order o) {
        return deliveryCost - o.deliveryCost;
    }
}
