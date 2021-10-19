package uk.ac.ed.inf;


/**
 * class storing the detail of a location represented by a w3w address
 */
public class Location {
    /** the country this location is in **/
    public final String country;
    /** the square represented by this location **/
    public final Square square;
    /** the nearest city to this location **/
    public final String nearestPlace;
    /** the long lat coordinate of the location **/
    public final LongLatSimple coordinates;
    /** the location in w3w format **/
    public final String words;
    /** the language of the name of the location **/
    public final String language;
    /** url to its location on the map of w3w server **/
    public final String map;


    /**
     * construct a location object with entries corresponding to the json files on w3w server
     * @param country the country this location is in
     * @param square the square represented by this location
     * @param nearestPlace the nearest city to this location
     * @param coordinates the long lat coordinate of the location
     * @param words the location in w3w format
     * @param language the language of the name of the location
     * @param map url to its location on the map of w3w server
     */
    public Location(String country, Square square, String nearestPlace, LongLatSimple coordinates, String words, String language, String map) {
        this.country = country;
        this.square = square;
        this.nearestPlace = nearestPlace;
        this.coordinates = coordinates;
        this.words = words;
        this.language = language;
        this.map = map;
    }
}
