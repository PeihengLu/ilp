package uk.ac.ed.inf;


/**
 * class storing the detail of a location represented by a w3w address
 */
public class Location {
    /** the country this location is in **/
    private final String country;
    /** the square represented by this location **/
    private final Square square;
    /** the nearest city to this location **/
    private final String nearestPlace;
    /** the long lat coordinate of the location **/
    private final LongLatSimple coordinates;
    /** the location in w3w format **/
    private final String words;
    /** the language of the name of the location **/
    private final String language;
    /** url to its location on the map of w3w server **/
    private final String map;


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

    /**
     * get the country this location is in
     * @return The country this location is in
     */
    public String getCountry() {
        return country;
    }

    /**
     * get the square represented by this location
     * @return the square represented by this location
     */
    public Square getSquare() {
        return square;
    }

    /**
     * get the nearest city to this location
     * @return the nearest city to this location
     */
    public String getNearestPlace() {
        return nearestPlace;
    }

    /**
     * get the long lat coordinate of the location
     * @return the long lat coordinate of the location
     */
    public LongLatSimple getCoordinates() {
        return coordinates;
    }

    /**
     * get the location in w3w format
     * @return the location in w3w format
     */
    public String getWords() {
        return words;
    }

    /**
     * get the language of the name of the location
     * @return the language of the name of the location
     */
    public String getLanguage() {
        return language;
    }

    /**
     * get the url to its location on the map of w3w server
     * @return url to its location on the map of w3w server
     */
    public String getMap() {
        return map;
    }
}
