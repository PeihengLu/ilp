package uk.ac.ed.inf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.json.JSONArray;
import org.json.JSONException;


/**
 * Class used to read Json files from our web server
 * Added org.json:json artifact for this class to function
 * Taken heavy inspiration from the following discussion:
 * https://stackoverflow.com/questions/4308554/simplest-way-to-read-json-from-a-url-in-java
 */
public class ReadJson {
    /**
     * read from a reader created by readJsonFromUrl into a String
     * @param reader reader created by method readJsonFromUrl
     * @return a string containing all content read by the reader
     */
    private static String readAll(BufferedReader reader) {
        StringBuilder buffer = new StringBuilder();
        // read all lines in the json file
        while (true) {
            String line;
            try {
                line = reader.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (line == null) {
                break;
            } else {
                buffer.append(line);
                buffer.append("\n");
            }
        }
        return buffer.toString();
    }

    /**
     * read a json file from a web server
     * @param url the address of the JSON file on the web server
     * @return the JSON object read from the file, in this case will be a JSON Array
     */
    public static JSONArray readJsonFromUrl(String url) {
        try {
            // open the url directed to the JSON file to read
            InputStream is = new URL(url).openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String jsonText = readAll(reader);;
            return new JSONArray(jsonText);
        } catch (JSONException | IOException ex) {
            System.err.println(ex);
            return null;
        }
    }
}
