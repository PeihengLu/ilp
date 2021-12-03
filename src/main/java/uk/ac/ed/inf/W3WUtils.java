package uk.ac.ed.inf;


import com.google.gson.Gson;

/**
 * class providing decoding functionality for W3W address
 */
public class W3WUtils {
    /** the url to the part of website where the w3w translation information is stored */
    public final String server;

    /**
     * initialize a W3WUtils object using the machine name and port of the web server
     * @param name the machine name of web server
     * @param port the port of the web server
     */
    public W3WUtils(String name, String port) {
        // address of the web server storing location information encoded in the
        // three words
        this.server = "HTTP://" + name + ":" + port + "/words/";;
    }


    /**
     * decode a w3w formatted location and parse it into a Location object holding all
     * the information stored on the server
     * @param word the w3w format location
     * @return null if error happens at connection or parsing, otherwise the Location object containing all the information from the server
     */
    public Location convertW3W(String word) {
        String[] threeWords = word.split("\\.");
        String url = server;
        for (String w: threeWords) {
            url += w + "/";
        }
        url += "details.json";

        // server connection and information retrieval
        ServerUtils connection = new ServerUtils(url);
        String response = connection.readStringFromUrl();
        if (response == null) {
            System.err.println("Trouble connecting to server");
            return null;
        }

        // parse the response from the server into a Location object
        Location loc = new Gson().fromJson(response, Location.class);
        if (loc == null) {
            System.err.println("Issue with parsing of JSON file");
            return null;
        }

        return loc;
    }
}
