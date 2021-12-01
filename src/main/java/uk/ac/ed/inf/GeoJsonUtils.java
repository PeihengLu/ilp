package uk.ac.ed.inf;

import com.mapbox.geojson.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * helper methods to read and write
 */
public class GeoJsonUtils {
    public final String noFlyZone;
    public final String landmarks;

    /**
     * Initialize a GeoJsonUtils object by setting the server address of
     * the part of website that stores W3W translation information
     * @param name the name of the server
     * @param port the port used to connect to the server
     */
    public GeoJsonUtils(String name, String port) {
        // url of the server location
        String server = "HTTP://" + name + ":" + port + "/buildings/";
        // location of the menus file on the server
        noFlyZone = server + "no-fly-zones.geojson";
        landmarks = server + "landmarks.geojson";
    }


    /**
     * read a geojson file from the server
     * @param url the address of the file to read
     * @return null if error occurs at connection,
     */
    public static List<Feature> readGeoJson(String url) {
        // start HTTP connection with the server
        ServerUtils connection = new ServerUtils(url);

        // the file content read from the menus json file on the web server
        String content = connection.readStringFromUrl();
        if (content == null) {
            System.err.println("Trouble connecting to server");
            return null;
        }
        FeatureCollection featureCollection = FeatureCollection.fromJson(content);
        List<Feature> features = featureCollection.features();
        return features;
    }


    /**
     * writing the path created to the output file corresponding to that day's order
     * @param featureCollection the LineString cast into a FeatureCollection for the purpose of
     *                          serialization
     * @param filename the name of the designated output file containing the path
     */
    public static void writeGeoJson(FeatureCollection featureCollection, String filename) {
        String json = featureCollection.toJson();

        try {
            FileWriter writer = new FileWriter(filename);
            writer.write(json);
            writer.close();
        } catch (IOException ex) {
            System.err.println("Error with writing to output file");
        }
    }
}
