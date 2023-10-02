package njit.JerSE;

import com.fasterxml.jackson.databind.ObjectMapper;
import njit.JerSE.api.ApiService;
import njit.JerSE.models.*;
import njit.JerSE.services.CheckerFrameworkCompiler;
import njit.JerSE.services.MethodReplacementService;
import njit.JerSE.utils.Configuration;
import njit.JerSE.services.OpenAIService;
import njit.JerSE.utils.JavaCodeParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * ASHE provides a means to interact with GPT to get suggestions for
 * correcting Java code.
 * <p>
 * This class utilizes the Checker Framework to compile Java code and detect
 * errors. If errors are found, it fetches suggestions from GPT to rectify
 * those errors, recompiles the code and overwrites the original code if
 * the suggestions result in code that compiles without errors using the
 * Checker Framework.
 */
public class ASHE {
    private static final Logger LOGGER = LogManager.getLogger(ASHE.class);

    /**
     * Initializes the configuration settings for accessing GPT API and
     * setting up prompts.
     */
    Configuration config = Configuration.getInstance();
    private final String API_KEY = config.getPropertyValue("llm.api.key");
    private final String API_URI = config.getPropertyValue("llm.api.uri");
    private final String GPT_SYSTEM = config.getPropertyValue("gpt.message.system");
    private final String GPT_USER = config.getPropertyValue("gpt.message.user");
    private final String GPT_SYSTEM_CONTENT = config.getPropertyValue("gpt.message.system.content");
    private final String PROMPT_START = config.getPropertyValue("gpt.prompt.start");
    private final String PROMPT_END = config.getPropertyValue("gpt.prompt.end");

    /**
     * Fixes Java code using suggestions from GPT.
     * <p>
     *
     * @param sourceFile the path to the Java class file to be corrected
     * @throws IOException              if there's an issue accessing the file or writing to it
     * @throws FileNotFoundException    if the provided file path does not point to a valid file
     * @throws IllegalArgumentException if the GPT response is unexpected
     * @throws InterruptedException     if the API call is interrupted
     */
    void fixJavaCodeUsingGPT(String sourceFile) throws IOException, FileNotFoundException, IllegalArgumentException, InterruptedException {
        MethodReplacementService methodReplacement = new MethodReplacementService();
        JavaCodeParser extractor = new JavaCodeParser();
        CheckerFrameworkCompiler checkerFrameworkCompiler = new CheckerFrameworkCompiler();
        String errorOutput = checkerFrameworkCompiler.compileWithCheckerFramework(sourceFile);

        if (errorOutput.isEmpty()) {
            LOGGER.info("No errors found in the file.");
            return;
        }

        LOGGER.warn("Errors found in the file.");
        LOGGER.warn(errorOutput);

        while (!errorOutput.isEmpty()) {

            String methodWithError = String.valueOf(extractor.extractFirstClassFromFile(sourceFile));

            String prompt = methodWithError +
                    "\n" +
                    PROMPT_START +
                    "\n" +
                    errorOutput +
                    "\n" +
                    PROMPT_END;

            String gptCorrection = fetchGPTCorrection(prompt);
            String codeBlock = extractor.extractJavaCodeBlockFromResponse(gptCorrection);

            if (codeBlock.isEmpty()) {
                LOGGER.error("Could not extract code block from GPT response.");
                return;
            }

            LOGGER.info("Code block extracted from GPT response." + System.lineSeparator() + codeBlock);

            if (!methodReplacement.replacePreviousMethod(sourceFile, codeBlock)) {
                LOGGER.error("Failed to write code to file.");
                return;
            }

            LOGGER.info("File written successfully. Recompiling with Checker Framework to check for additional warnings...");

            // This will be checked at the start of the next iteration
            errorOutput = checkerFrameworkCompiler.compileWithCheckerFramework(sourceFile);

            if (!errorOutput.isEmpty()) {
                LOGGER.warn("Additional error(s) found after recompiling.");
            }
        }

        LOGGER.info("No more errors found in the file.");
        LOGGER.info("Exiting...");
    }

    /**
     * Fetches correction for Java code from GPT.
     *
     * @param prompt the prompt to be provided to GPT
     * @return a string representation of GPT's response containing the
     * code correction
     * @throws IOException           if there's an error during the API call or processing the response
     * @throws IllegalStateException if the response from the API is not as expected
     * @throws InterruptedException  if the API call is interrupted
     */
    private String fetchGPTCorrection(String prompt) throws IOException, IllegalStateException, InterruptedException {
        LOGGER.debug("Fetching GPT correction with prompt: {}", prompt);

        ApiService openAIService = new OpenAIService();
        ObjectMapper objectMapper = new ObjectMapper();
        HttpClient client = HttpClient.newHttpClient();

        GPTRequest gptRequest = createGptRequestObject(prompt);
        String apiRequestBody = objectMapper.writeValueAsString(gptRequest);

        HttpRequest request = openAIService.apiRequest(API_KEY, API_URI, apiRequestBody);
        HttpResponse<String> httpResponse = openAIService.apiResponse(request, client);

        if (httpResponse.statusCode() == 200) {
            GPTResponse gptResponse = objectMapper.readValue(httpResponse.body(), GPTResponse.class);
            LOGGER.info("Successfully retrieved GPT Prompt response.");
            return gptResponse.choices()[gptResponse.choices().length - 1].message().content();
        } else {
            String errorMsg = "Error:" + System.lineSeparator() + httpResponse.statusCode() + " " + httpResponse.body();
            LOGGER.error(errorMsg);
            return errorMsg;
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
        LOGGER.debug("Creating GPT request object with prompt: {}", prompt);

        GPTMessage systemMessage = new GPTMessage(GPT_SYSTEM, GPT_SYSTEM_CONTENT);
        GPTMessage userMessage = new GPTMessage(GPT_USER, prompt);
        GPTMessage[] messages = new GPTMessage[]{systemMessage, userMessage};

        LOGGER.debug("GPT request object created successfully.");
        return new GPTRequest(GPTModel.GPT_4, messages);
    }
}
