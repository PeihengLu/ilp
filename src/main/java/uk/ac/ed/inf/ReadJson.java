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
     * @return the JSON Array read from the file
     */
    public JSONArray readJsonFromUrl() {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("Server connection error");
                return null;
            }
            return new JSONArray(response.body());
        } catch (IOException | InterruptedException | JSONException ex) {
            System.err.println(ex.getMessage());
            return null;
        }
    }
}
