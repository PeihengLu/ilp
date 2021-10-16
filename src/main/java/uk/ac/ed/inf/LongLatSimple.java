package uk.ac.ed.inf;


import java.util.List;

/**
 * created for Gson parsing, as the parameter name is different from longitude and latitude in LongLat
 */
public class LongLatSimple {
    public final double lng;
    public final double lat;

    public LongLatSimple(double lng, double lat) {
        this.lng = lng;
        this.lat = lat;
    }
}
