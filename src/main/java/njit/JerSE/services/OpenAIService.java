package njit.JerSE.services;

import njit.JerSE.api.ApiService;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Service implementation for interacting with the OpenAI API.
 * <p>
 * This service provides methods for constructing API requests to OpenAI and
 * retrieving API responses using the provided HTTP client.
 */
public class OpenAIService implements ApiService {

    /**
     * Constructs an API request to OpenAI with the provided parameters.
     *
     * @param apiKey the API key for authorization
     * @param apiUri the API endpoint URI
     * @param apiRequestBody the body to send with the request
     * @return a constructed HttpRequest ready to be sent to the OpenAI API
     */
    @Override
    public HttpRequest apiRequest(String apiKey, String apiUri, String apiRequestBody) {
        System.out.println("Making API Request...");
        return HttpRequest.newBuilder()
                .uri(URI.create(apiUri))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(apiRequestBody))
                .build();
    }

    /**
     * Sends the provided HttpRequest and retrieves the API response.
     *
     * @param request the HttpRequest to send
     * @param client the HttpClient used to send the request
     * @return the HttpResponse containing the APIs response
     * @throws IOException if an I/O error occurs when sending or receiving
     * @throws InterruptedException If the operation is interrupted
     */
    @Override
    public HttpResponse<String> apiResponse(HttpRequest request, HttpClient client) throws IOException, InterruptedException {
        System.out.println("Getting API Response...");
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
