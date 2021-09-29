package uk.ac.ed.inf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Class used to read Json files from our web server
 */
public class ReadJson {
    private final String url;
    private static final HttpClient client = HttpClient.newHttpClient();

    /**
     *
     * @param url the address of the JSON file on the web server
     */
    public ReadJson(String url) {
        this.url = url;
    }

    /**
     * read a json file from a web server
     * @return the content read from the file as a String, returns "" if problems occur at connection or
     *         reading stage
     */
    private String readStringFromUrl() {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("Server connection error");
                return "";
            }
            return response.body();
        } catch (IOException | InterruptedException | JSONException ex) {
            System.err.println(ex.getMessage());
            return "";
        }
    }


    /**
     * Parse the file content into a Json array
     * @return JSONArray object read from the file, returns null if errors occurred at reading or parsing
     */
    public JSONArray getJsonArray() {
        String response = readStringFromUrl();
        if (response.equals("")) {
            System.err.println("Failed to read Json file");
            return null;
        }
        try {
            return new JSONArray(response);
        } catch (JSONException e) {
            System.err.println(e.getMessage());
            return null;
        }
    }


    /**
     * Parse the file content into a Json object
     * @return JSON object read from the file, returns null if errors occurred at reading or parsing
     */
    public JSONObject getJsonObject() {
        String response = readStringFromUrl();
        if (response.equals("")) {
            System.err.println("Failed to read Json file");
            return null;
        }
        try {
            return new JSONObject(response);
        } catch (JSONException e) {
            System.err.println(e.getMessage());
            return null;
        }
    }
}
