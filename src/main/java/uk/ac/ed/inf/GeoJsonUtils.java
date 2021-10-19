package uk.ac.ed.inf;

import com.mapbox.geojson.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * helper methods to read and write
 */
public class GeoJsonUtils {
    public final String noFlyZone;
    public final String landmarks;

    public GeoJsonUtils(String name, String port) {
        // url of the server location
        String server = "HTTP://" + name + ":" + port + "/buildings/";
        // location of the menus file on the server
        noFlyZone = server + "no-fly-zones.geojson";
        landmarks = server + "landmarks.geojson";
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


    /**
     * effectively check if a line passes through the outer perimeter of a Polygon
     * @param p1 one end of the path (line segment) to check
     * @param p2 another end of the path to check
     * @param polygon the polygon representing one of the no-fly zones
     * @return true if the path intercept with the outer perimeter of the no-fly zone
     */
    public static boolean pathInterceptPolygon(Point p1, Point p2, Polygon polygon) {
        LineString outer = polygon.outer();
        List<Point> lines = outer.coordinates();
        // check the interception of the path with all line segments of the perimeter
        for (int i = 0; i < lines.size(); i ++) {
            if (checkIntercept(p1, p2, lines.get(i), lines.get((i + 1) % lines.size()))) return true;
        }
        return false;
    }


    /**
     * check if two lines intercept with each other, ingenious solution from Bryce Boe:
     * https://bryceboe.com/2006/10/23/line-segment-intersection-algorithm/
     * using the counterclockwise helper function to deal with interception problems, cannot
     * deal with collinearity but suffice for our use
     * @param A one endpoint of line 1
     * @param B another endpoint of line 1
     * @param C one endpoint of line 2
     * @param D another endpoint of line 2
     * @return true if line 1 intercept with line 2
     */
    private static boolean checkIntercept(Point A, Point B, Point C, Point D) {
        return counterClockWise(A, C, D) != counterClockWise(B, C, D) &&
                counterClockWise(A, B, C) != counterClockWise(A, B, D);
    }


    /**
     * determines if three points A, B, C are listed in a counterclockwise order
     * @param A Point A
     * @param B Point B
     * @param C Point C
     * @return true if A, B, C are listed in a counterclockwise order
     */
    private static boolean counterClockWise(Point A, Point B, Point C) {
        double Ax = A.coordinates().get(0);
        double Ay = A.coordinates().get(1);
        double Bx = B.coordinates().get(0);
        double By = B.coordinates().get(1);
        double Cx = C.coordinates().get(0);
        double Cy = C.coordinates().get(1);
        // if slope of AB is less than the slope of AC, then A, B, C are listed in a counterclockwise orientation
        // use this form to avoid problems of divide by 0 when encounters vertical lines
        return (Cy - Ay) * (Bx - Ax) > (By - Ay) * (Cx - Ax);
    }
}
