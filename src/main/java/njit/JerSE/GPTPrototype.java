package njit.JerSE;

import com.fasterxml.jackson.databind.ObjectMapper;
import njit.JerSE.api.ApiService;
import njit.JerSE.config.Configuration;
import njit.JerSE.models.GPTChatResponse;
import njit.JerSE.models.GPTMessage;
import njit.JerSE.models.GPTModel;
import njit.JerSE.models.GPTRequest;
import njit.JerSE.services.FileWatcher;
import njit.JerSE.services.OpenAIService;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GPTPrototype {
    Configuration config = new Configuration();
    private final String API_KEY = config.getPropertyValue("llm.api.key"); // Replace with your actual API key
    private final String EXAMPLE_METHOD = config.getPropertyValue("example.method");

    void runGPTPrototype() throws Exception {
        StringBuilder prompt = new StringBuilder();

        // TODO: DO NOT HARDCODE THESE PROMPTS IN THIS FILE
        // TODO: Get the actual method from the verification minimizer
        // EXAMPLE_METHOD is the specific test case that I am using to get this running
        prompt.append(EXAMPLE_METHOD);
        prompt.append("\nmy compiler is running with the Checker Framework's Resource Leak Checker, and it issues an error about the program above. Here's the error:");
        prompt.append("\n");
        FileWatcher fileWatcher = new FileWatcher();
        String errors = fileWatcher.watchForErrors();
        if (errors != null) {
            prompt.append(errors);
            prompt.append("\n");
            prompt.append("Can you rewrite the program to avoid this error from the compiler? Ensure to include the entire method in your response and also add comments for each line you modify.");

            // FIXME: READ ME
            // NOTE: I have found that the LLM is giving proper feedback (although maybe not the prettiest code),
            //       the answers it provides are eliminating the Checker Framework warnings/errors, thus
            //       allowing the Example Project to compile and run without any errors.
            //       We are also getting detailed comments in the code that may be helpful when a developer looks at the changes.
            System.out.println(getGPTResponseForPrompt(prompt.toString()));

            // TODO: Implement future work here
            /*
             * This is where we will call a helper method to overwrite the file,
             * specifically the method fed to the LLM prompt, with the code response
             *
             * This method is just an example of what we will do
             * overwriteFileHelperMethod();
             */
        }
    }

    private String getGPTResponseForPrompt(String prompt) throws Exception {
        ApiService openAIService = new OpenAIService();
        ObjectMapper objectMapper = new ObjectMapper();
        HttpClient client = HttpClient.newHttpClient();

        GPTMessage message = new GPTMessage("user", prompt);
        GPTMessage[] messages = new GPTMessage[]{message};

        GPTRequest gptRequest = new GPTRequest(
                GPTModel.GPT_3_5_TURBO.getModel(),
                GPTModel.GPT_3_5_TURBO.getTemperature(),
                GPTModel.GPT_3_5_TURBO.getMax_tokens(),
                GPTModel.GPT_3_5_TURBO.getTop_p(),
                GPTModel.GPT_3_5_TURBO.getFrequency_penalty(),
                GPTModel.GPT_3_5_TURBO.getPresence_penalty(),
                messages
        );

        String input = objectMapper.writeValueAsString(gptRequest);

        HttpRequest request = openAIService.apiRequest(API_KEY, input);
        HttpResponse<String> httpResponse = openAIService.apiResponse(request, client);

        if (httpResponse.statusCode() == 200) {
            GPTChatResponse chatResponse = objectMapper.readValue(httpResponse.body(), GPTChatResponse.class);
            return chatResponse.getChoices()[chatResponse.getChoices().length - 1].getMessage().getContent();
        } else {
            return "Error:\n" + httpResponse.statusCode() + " " + httpResponse.body();
        }
    }

    /* // TODO: Create a better name
     * private String parseLLMPrompt() {
     *    regex only the code from the prompt - we must come up with a better solution long term
     *    return the code as a string
     * }
     */

    /* // TODO: Create a better name
     * private void overwriteFileHelperMethod() {
     *    Get the .java / .class file from minimized project
     *    parsePrompt = parseLLMPrompt();
     *    parsePrompt overwrite the method in the file
     * }
     */
}
