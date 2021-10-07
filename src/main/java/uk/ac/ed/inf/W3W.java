package uk.ac.ed.inf;


import com.google.gson.Gson;

/**
 * class providing decoding functionality for what three words address
 */
public class W3W {
    /**
     * decode a w3w formatted location and parse it into a Location object holding all
     * the information stored on the server
     * @param name the machine name of the online server
     * @param port the port of connection to the online server
     * @param word the w3w format location
     * @return null if error happens at connection or parsing, otherwise the Location object containing all the information from the server
     */
    public static Location convertW3W(String name, String port, String word) {
        String[] threeWords = word.split("\\.");
        // address of the web server storing location information encoded in the
        // three words
        String url = "HTTP://" + name + ":" + port + "/words/";
        for (String w: threeWords) {
            url += w + "/";
        }
        url += "details.json";

        // server connection and information retrieval
        ConnectServer connection = new ConnectServer(url);
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
