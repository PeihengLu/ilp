package uk.ac.ed.inf;


/**
 * storing the information of a square by storing the Longitude and
 * Latitude of the southWest and northEast corner
 */
public class Square {
    /** southwest corner of this square **/
    public final LongLatSimple southWest;
    /** northeast corner of this square **/
    public final LongLatSimple northEast;

    /**
     * construct a Square object
     * @param southWest southwest corner of the square
     * @param northEast northeast corner of the square
     */
    public Square(LongLatSimple southWest, LongLatSimple northEast) {
        this.southWest = southWest;
        this.northEast = northEast;
    }
}
