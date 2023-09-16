package njit.JerSE;

import com.fasterxml.jackson.databind.ObjectMapper;
import njit.JerSE.api.ApiService;
import njit.JerSE.models.*;
import njit.JerSE.services.CheckerFrameworkCompiler;
import njit.JerSE.utils.Configuration;
import njit.JerSE.services.JavaMethodOverwrite;
import njit.JerSE.services.OpenAIService;
import njit.JerSE.utils.JavaCodeParser;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * GPTPrototype provides a means to interact with GPT to get suggestions for
 * correcting Java code.
 * <p>
 * This class utilizes the Checker Framework to compile Java code and detect
 * errors. If errors are found, it fetches suggestions from GPT to rectify
 * those errors, recompiles the code and overwrites the original code if
 * the suggestions are valid.
 */
public class GPTPrototype {
    /**
     * Initializes the configuration settings for accessing GPT API and
     * setting up prompts.
     */
    Configuration config = new Configuration();
    private final String API_KEY = config.getPropertyValue("llm.api.key");
    private final String API_URI = config.getPropertyValue("llm.api.uri");
    private final String GPT_SYSTEM = config.getPropertyValue("gpt.message.system");
    private final String GPT_USER = config.getPropertyValue("gpt.message.user");
    private final String GPT_SYSTEM_CONTENT = config.getPropertyValue("gpt.message.system.content");
    private final String PROMPT_INTRO = config.getPropertyValue("gpt.prompt.intro");
    private final String PROMPT_OUTRO = config.getPropertyValue("gpt.prompt.outro");

    /**
     * Fixes Java code using suggestions from GPT.
     *
     * @param classPath the path to the Java class file to be corrected
     * @throws IOException if there's an issue accessing the file or writing to it
     * @throws FileNotFoundException if the provided file path does not point to a valid file
     * @throws IllegalArgumentException if the GPT response is unexpected
     * @throws InterruptedException if the API call is interrupted
     */
    void fixJavaCodeUsingGPT(String classPath) throws IOException, FileNotFoundException, IllegalArgumentException, InterruptedException {
        JavaMethodOverwrite javaMethodOverwrite = new JavaMethodOverwrite();
        JavaCodeParser extractor = new JavaCodeParser();
        CheckerFrameworkCompiler checkerFrameworkCompiler = new CheckerFrameworkCompiler();
        String errorOutput = checkerFrameworkCompiler.compileWithCheckerFramework(classPath);

        if (errorOutput.isEmpty()) {
            System.out.println("No errors found in the file.");
            return; // No errors, so we exit early
        }

        System.out.println("Errors found in the file.");

        while (!errorOutput.isEmpty()) {

            String exampleMethod = String.valueOf(extractor.extractFirstClassFromFile(classPath));

            String prompt = exampleMethod +
                    "\n" +
                    PROMPT_INTRO +
                    "\n" +
                    errorOutput +
                    "\n" +
                    PROMPT_OUTRO;

            String gptResponse = fetchGPTCorrection(prompt);
            String codeBlock = extractor.extractJavaCodeBlockFromResponse(gptResponse);

            if (codeBlock.isEmpty()) {
                System.out.println("Could not extract code block from GPT response.");
                return; // Exit the function if no code block is extracted
            }

            if (!javaMethodOverwrite.writeToFile(classPath, codeBlock)) {
                System.out.println("Failed to write code to file.");
                return; // Exit the function if writing to file fails
            }

            System.out.println("File written successfully. Recompiling with Checker Framework to check for additional warnings...");

            // This will be checked at the start of the next iteration
            errorOutput = checkerFrameworkCompiler.compileWithCheckerFramework(classPath);

            if (!errorOutput.isEmpty()) {
                System.out.println("Additional error(s) found after recompiling.");
            }
        }

        System.out.println("No more errors found in the file.");
        System.out.println("Exiting...");
    }

    /**
     * Fetches correction for Java code from GPT.
     *
     * @param prompt the prompt to be provided to GPT
     * @return a string representation of GPT's response containing the
     * code correction
     * @throws IOException if there's an error during the API call or processing the response
     * @throws IllegalStateException if the response from the API is not as expected
     * @throws InterruptedException if the API call is interrupted
     */
    private String fetchGPTCorrection(String prompt) throws IOException, IllegalStateException, InterruptedException {
        ApiService openAIService = new OpenAIService();
        ObjectMapper objectMapper = new ObjectMapper();
        HttpClient client = HttpClient.newHttpClient();

        GPTRequest gptRequest = createGptRequestObject(prompt);
        String apiRequestBody = objectMapper.writeValueAsString(gptRequest);

        HttpRequest request = openAIService.apiRequest(API_KEY, API_URI, apiRequestBody);
        HttpResponse<String> httpResponse = openAIService.apiResponse(request, client);

        if (httpResponse.statusCode() == 200) {
            GPTChatResponse chatResponse = objectMapper.readValue(httpResponse.body(), GPTChatResponse.class);
            System.out.println("Successfully retrieved GPT Prompt response.");
            return chatResponse.choices()[chatResponse.choices().length - 1].message().content();
        } else {
            return "Error:\n" + httpResponse.statusCode() + " " + httpResponse.body();
        }
    }

    /**
     * Creates a GPT request object based on the provided prompt.
     *
     * @param prompt the prompt to be provided to GPT
     * @return a GPTRequest object configured with the required parameters
     * for the GPT API call
     */
    private GPTRequest createGptRequestObject(String prompt) {
        GPTMessage systemMessage = new GPTMessage(GPT_SYSTEM, GPT_SYSTEM_CONTENT);
        GPTMessage userMessage = new GPTMessage(GPT_USER, prompt);
        GPTMessage[] messages = new GPTMessage[]{systemMessage, userMessage};

        return new GPTRequest(
                GPTModel.GPT_4.getModel(),
                GPTModel.GPT_4.getTemperature(),
                GPTModel.GPT_4.getMax_tokens(),
                GPTModel.GPT_4.getTop_p(),
                GPTModel.GPT_4.getFrequency_penalty(),
                GPTModel.GPT_4.getPresence_penalty(),
                messages
        );
    }
}
