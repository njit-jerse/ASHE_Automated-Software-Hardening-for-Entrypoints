package njit.JerSE.api;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public interface ApiService {
    HttpRequest apiRequest(String apiKey, String input);
    HttpResponse<String> apiResponse(HttpRequest request, HttpClient client) throws Exception;
}
