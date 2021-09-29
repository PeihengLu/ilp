package uk.ac.ed.inf;


/**
 * Stores location information of the drone and possible location of interest,
 * also containing useful functions for calculating the movement of the drone
 */
public class LongLat {
    /** longitude and latitude of current object */
    private final double longitude;
    private final double latitude;

    // Below are some possibly useful coordinates of key locations
    /** Forest Hill on Top Left */
    public static final LongLat FH = new LongLat(-3.192473, 55.946233);
    /** KFC on Top Right */
    public static final LongLat KFC = new LongLat(-3.184319, 55.946233);
    /** Top of the Meadows on Bottom Left */
    public static final LongLat TOM = new LongLat(-3.192473, 55.942617);
    /** Buccleuch St Bus Stop on Bottom Right */
    public static final LongLat BBS = new LongLat(-3.184319, 55.942617);

    /** distance for one movement in degrees */
    private static final double distance = 0.00015;


    public LongLat(double longitude, double latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }

    /**
     * check if the coordinate of this LongLat is within the confinement of FH, KFC, TOM and BBS
     * @return true if it's within the confinement, false otherwise
     */
    public boolean isConfined() {
        // check the coordinate against the key points on the upper left and lower right corner
        return this.longitude < BBS.longitude &&
               this.longitude > FH.longitude &&
               this.latitude < FH.latitude &&
               this.latitude > BBS.latitude;
    }

    /**
     * calculate the distance between this coordinate and another LongLat object
     * @param other the LongLat object to which distance we want to know
     * @return the distance between this and other in degrees
     */
    public double distanceTo(LongLat other) {
        // calculate use the Pythagorean function, ignoring the curvature of the ground
        return Math.sqrt(Math.pow(other.longitude - this.longitude, 2) + Math.pow(other.latitude - this.latitude, 2));
    }

    /**
     * check if this coordinate is close to other LongLat object (distance strictly less than 0.00015)
     * @param other another LongLat object whose closeness to this we wish to know
     * @return true if distance is strictly less than 0.00015, false otherwise;
     */
    public boolean closeTo(LongLat other) {
        // close to is defined by distance strictly less than 0.00015 degrees
        return distanceTo(other) < 0.00015;
    }

    /**
     * calculate the position after moving or hovering for one step
     * @param angle the angle to move towards, -999 if the drone is hovering
     * @return the new LongLat object after movement
     */
    public LongLat nextPosition(int angle) {
        // the drone is hovering when angle if -999
        if (angle == -999) return this;
        return new LongLat(this.longitude + distance*Math.cos(Math.toRadians(angle)), this.latitude + distance*Math.sin(Math.toRadians(angle)));
    }

    /**
     * return the latitude of the current location
     * @return latitude of current location
     */
    public double getLatitude() {
        return latitude;
    }


    /**
     * return the longitude of the current location
     * @return longitude of current location
     */
    public double getLongitude() {
        return longitude;
    }
}
