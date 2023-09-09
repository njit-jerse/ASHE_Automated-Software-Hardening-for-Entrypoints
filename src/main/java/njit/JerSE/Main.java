package njit.JerSE;

import com.fasterxml.jackson.databind.ObjectMapper;
import njit.JerSE.api.ApiService;
import njit.JerSE.models.GPTChatResponse;
import njit.JerSE.models.GPTMessage;
import njit.JerSE.models.GPTRequest;
import njit.JerSE.services.OpenAIService;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Main {
    public static void main(String[] args) throws Exception {
        GPTPrototype gptPrototype = new GPTPrototype();
        gptPrototype.runGPTPrototype();
    }
}
