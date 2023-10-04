package njit.JerSE;

import njit.JerSE.services.CheckerFrameworkCompiler;
import njit.JerSE.services.GPTApiClient;
import njit.JerSE.services.MethodReplacementService;
import njit.JerSE.utils.Configuration;
import njit.JerSE.utils.JavaCodeParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

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
    void fixJavaCodeUsingGPT(String sourceFile) throws IOException, FileNotFoundException, IllegalArgumentException, InterruptedException, ExecutionException, TimeoutException {
        MethodReplacementService methodReplacement = new MethodReplacementService();
        JavaCodeParser extractor = new JavaCodeParser();
        CheckerFrameworkCompiler checkerFrameworkCompiler = new CheckerFrameworkCompiler();
        String errorOutput = checkerFrameworkCompiler.compileWithCheckerFramework(sourceFile);
        GPTApiClient gptApiClient = new GPTApiClient();

        if (errorOutput.isEmpty()) {
            LOGGER.info("No errors found in the file.");
            return;
        }

        LOGGER.warn("Errors found in the file:" + System.lineSeparator() + errorOutput);

        while (!errorOutput.isEmpty()) {

            String methodWithError = String.valueOf(extractor.extractFirstClassFromFile(sourceFile));

            String prompt = methodWithError +
                    "\n" +
                    PROMPT_START +
                    "\n" +
                    errorOutput +
                    "\n" +
                    PROMPT_END;

            String gptCorrection = gptApiClient.fetchGPTResponse(prompt);
            String codeBlock = extractor.extractJavaCodeBlockFromResponse(gptCorrection);

            if (codeBlock.isEmpty()) {
                LOGGER.error("Could not extract code block from GPT response.");
                return;
            }

            LOGGER.info("Code block extracted from GPT response:" + System.lineSeparator() + codeBlock);

            if (!methodReplacement.replacePreviousMethod(sourceFile, codeBlock)) {
                LOGGER.error("Failed to write code to file.");
                return;
            }

            LOGGER.info("File written successfully. Recompiling with Checker Framework to check for additional warnings...");

            // This will be checked at the start of the next iteration
            errorOutput = checkerFrameworkCompiler.compileWithCheckerFramework(sourceFile);

            if (!errorOutput.isEmpty()) {
                LOGGER.warn("Additional error(s) found after recompiling:" + System.lineSeparator() + errorOutput);
            }
        }

        LOGGER.info("No more errors found in the file.");
        LOGGER.info("Exiting...");
    }
}