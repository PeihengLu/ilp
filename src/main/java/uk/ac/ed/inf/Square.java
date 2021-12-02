package uk.ac.ed.inf;


/**
 * storing the information of a square by storing the Longitude and
 * Latitude of the southWest and northEast corner, necessary for Gson to
 * parse W3W Json file
 */
public class Square {
    /** southwest corner of this square **/
    public final LongLat southWest;
    /** northeast corner of this square **/
    public final LongLat northEast;

    /**
     * construct a Square object
     * @param southWest southwest corner of the square
     * @param northEast northeast corner of the square
     */
    public Square(LongLat southWest, LongLat northEast) {
        this.southWest = southWest;
        this.northEast = northEast;
    }
}
