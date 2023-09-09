package njit.JerSE.services;

import njit.JerSE.api.ApiService;
import njit.JerSE.config.Configuration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Properties;

public class OpenAIService implements ApiService {
    Configuration config = new Configuration();
    private final String API_URI = config.getPropertyValue("llm.api.uri");
    @Override
    public HttpRequest apiRequest(String apiKey, String input) {
        return HttpRequest.newBuilder()
                .uri(URI.create(API_URI))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(input))
                .build();
    }

    // TODO: Throw a more specific exception
    @Override
    public HttpResponse<String> apiResponse(HttpRequest request, HttpClient client) throws Exception {
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
