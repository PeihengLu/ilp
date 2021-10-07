package uk.ac.ed.inf;


/**
 * storing the information of a square by storing the Longitude and
 * Latitude of the southWest and northEast corner
 */
public class Square {
    /** southwest corner of this square **/
    private final LongLat southWest;
    /** northeast corner of this square **/
    private final LongLat northEast;

    /**
     * construct a Square object
     * @param southWest southwest corner of the square
     * @param northEast northeast corner of the square
     */
    public Square(LongLat southWest, LongLat northEast) {
        this.southWest = southWest;
        this.northEast = northEast;
    }

    /**
     * get the coordinate of the southwest corner of the square
     * @return the coordinate of the southwest corner of the square stored in a LongLat object
     */
    public LongLat getSouthWest() {
        return southWest;
    }

    /**
     * get the coordinate of the northeast corner of the square
     * @return the coordinate of the northeast corner of the square stored in a LongLat object
     */
    public LongLat getNorthEast() {
        return northEast;
    }
}
