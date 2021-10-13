package uk.ac.ed.inf;

import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Feature;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * helper methods to read and write
 */
public class GeoJSON {
    public final String noFlyZone;
    public final String landmarks;

    public GeoJSON(String name, String port) {
        // url of the server location
        String server = "HTTP://" + name + ":" + port + "/buildings/";
        // location of the menus file on the server
        noFlyZone = server + "no-fly-zones.geojson";
        landmarks = server + "landmarks.geojson";
        System.out.println(noFlyZone);
        System.out.println(landmarks);
    }

    public static List<Feature> readGeoJson(String url) {
        // start HTTP connection with the server
        ConnectServer connection = new ConnectServer(url);

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
