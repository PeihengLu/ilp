package uk.ac.ed.inf;

import com.mapbox.geojson.*;

import java.util.ArrayList;
import java.util.List;

/**
 * The lunch delivery drone control system
 */
public class App 
{
    public static void main( String[] args )
    {
        // fetch the commandline arguments and store them in variables
        String day = args[0];
        String month = args[1];
        String year = args[2];
        String port = args[3];
        String dbPort = args[4];
        // name of the server for connection
        String name = "localhost";
        // name of the output geojson file storing the path
        String outputFile = String.join("-", "drone", day, month, year) + ".geojson";

        // acquires the no-fly-zones and landmarks
        GeoJSON geoJSON = new GeoJSON(name, port);

        List<Feature> nfz = GeoJSON.readGeoJson(geoJSON.noFlyZone);
        List<Polygon> noFlyZones = new ArrayList<Polygon>();
        assert nfz != null;
        for (Feature f: nfz) {
            if (f.geometry() instanceof Polygon) {
                noFlyZones.add((Polygon) f.geometry());
            }
        }

        List<Feature> lm = GeoJSON.readGeoJson(geoJSON.landmarks);

        GeoJSON.writeGeoJson(FeatureCollection.fromFeatures(nfz), outputFile);

//        LineString result;
//        Feature line = Feature.fromGeometry(result);
//        FeatureCollection path = FeatureCollection.fromFeature(line);


    }
}
