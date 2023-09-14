package njit.JerSE.api;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * The ApiService interface provides methods for interacting with an API.
 * It defines methods to create API requests and fetch API responses.
 */
public interface ApiService {

    /**
     * Creates an API request.
     *
     * @param apiKey          The API key required for the request.
     * @param apiUri          The URI endpoint for the API request.
     * @param apiRequestBody  The body content for the API request.
     * @return The constructed {@link HttpRequest} object.
     */
    HttpRequest apiRequest(String apiKey, String apiUri, String apiRequestBody);

    /**
     * Retrieves the API response based on a given request.
     *
     * @param request The {@link HttpRequest} object.
     * @param client  The {@link HttpClient} to send the request.
     * @return The {@link HttpResponse} object containing the API's response as a string.
     * @throws IOException If there's a network or general I/O error.
     * @throws InterruptedException If the request is interrupted.
     */
    HttpResponse<String> apiResponse(HttpRequest request, HttpClient client) throws IOException, InterruptedException;
}
