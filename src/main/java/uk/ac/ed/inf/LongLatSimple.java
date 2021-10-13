package uk.ac.ed.inf;

public class LongLatSimple {
    public final double lng;
    public final double lat;

    public LongLatSimple(double lng, double lat) {
        this.lng = lng;
        this.lat = lat;
    }

    public double getLongitude() {
        return lng;
    }

    public double getLatitude() {
        return lat;
    }
}
