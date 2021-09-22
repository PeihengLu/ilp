package uk.ac.ed.inf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
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
     * @return a string read by the reader
     */
    private static String readAll(Reader reader) {
        StringBuilder sb = new StringBuilder();
        int cp;
        try {
            while ((cp = reader.read()) != -1) {
                sb.append((char) cp);
            }
        } catch (IOException ex) {
            System.err.println(ex);
        }
        return sb.toString();
    }

    public static JSONArray readJsonFromUrl(String url) {
        try {
            InputStream is = new URL(url).openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String jsonText = readAll(reader);
            return new JSONArray(jsonText);
        } catch (JSONException | IOException ex) {
            System.err.println(ex);
            return null;
        }
    }
}
