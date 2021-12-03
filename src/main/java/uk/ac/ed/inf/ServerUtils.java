package uk.ac.ed.inf;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


/**
 * Class used to read files from our web server
 */
public class ServerUtils {
    /** url directed to the file to read */
    private final String url;
    /** static http client for connection */
    private static final HttpClient client = HttpClient.newHttpClient();

    /**
     * constructor that assigns value to the url attribute
     * @param url the address of the file on the web server
     */
    public ServerUtils(String url) {
        this.url = url;
    }

    /**
     * read a file from a web server
     * @return the content read from the file as a String, returns null if problems occur at connection
     */
    public String readStringFromUrl() {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("Server connection error");
                return null;
            }
            return response.body();
        } catch (IOException | InterruptedException ex) {
            System.err.println(ex.getMessage());
            return null;
        }
    }
}
