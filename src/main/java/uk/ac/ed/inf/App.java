package uk.ac.ed.inf;

import com.mapbox.geojson.*;
import com.mapbox.geojson.Polygon;

import java.util.ArrayList;
import java.util.List;

/**
 * The lunch delivery drone control system
 */
public class App 
{
    public static void main( String[] args )
    {
        if (args.length != 5) {
            System.err.println("You should enter 5 arguments: day, month, year, webserver port and database port!");
        }
        // fetch the commandline arguments and store them in variables
        String day = args[0];
        String month = args[1];
        String year = args[2];
        String port = args[3];
        String dbPort = args[4];
        // name of the server for connection
        String name = "localhost";
        // name of the output geojson file storing the path
        String outputFile = "../" + String.join("-", "drone", day, month, year) + ".geojson";

        // create a menus object
        Menus menus = new Menus(name, port);

        // acquires the no-fly-zones and landmarks
        GeoJsonUtils geoJsonUtils = new GeoJsonUtils(name, port);

        List<Feature> nfz = GeoJsonUtils.readGeoJson(geoJsonUtils.noFlyZone);
        List<Polygon> noFlyZones = new ArrayList<Polygon>();
        assert nfz != null;
        for (Feature f: nfz) {
            if (f.geometry() instanceof Polygon) {
                noFlyZones.add((Polygon) f.geometry());
            }
        }

        List<Feature> lm = GeoJsonUtils.readGeoJson(geoJsonUtils.landmarks);

        GeoJsonUtils.writeGeoJson(FeatureCollection.fromFeatures(nfz), outputFile);

//        LineString result;
//        Feature line = Feature.fromGeometry(result);
//        FeatureCollection path = FeatureCollection.fromFeature(line);


    }
}
