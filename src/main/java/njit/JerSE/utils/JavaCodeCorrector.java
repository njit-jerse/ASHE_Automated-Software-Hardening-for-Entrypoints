package njit.JerSE.utils;

import njit.JerSE.ASHE;
import njit.JerSE.services.CheckerFrameworkCompiler;
import njit.JerSE.services.GPTApiClient;
import njit.JerSE.services.MethodReplacementService;
import njit.JerSE.services.SpeciminTool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Responsible for conducting Java code minimization and correction
 * using a combination of the SPECIMIN tool and GPT powered error
 * correction, integrated with the Checker Framework to validate code
 * integrity throughout the process.
 * <p>
 * The overarching mechanism aids in minimizing code whilst maintaining its
 * functionality, using AI-driven techniques to suggest and implement corrections.
 * It is fundamental in the process of optimizing Java code through
 * method minimization and automated error rectification.
 */
public class JavaCodeCorrector {

    private static final Logger LOGGER = LogManager.getLogger(ASHE.class);

    Configuration config = Configuration.getInstance();
    private final String PROMPT_START = config.getPropertyValue("gpt.prompt.start");
    private final String PROMPT_END = config.getPropertyValue("gpt.prompt.end");

    /**
     * Utilizes GPT API to attempt to fix errors in the target Java file.
     *
     * @param targetFile The path to the Java file to be corrected.
     * @return true if errors were successfully corrected; false otherwise.
     * @throws IOException, FileNotFoundException, IllegalArgumentException, InterruptedException, ExecutionException, TimeoutException
     */
    public boolean fixTargetFileErrorsWithGPT(String targetFile)
            throws IOException, IllegalArgumentException,
            InterruptedException, ExecutionException, TimeoutException {

        MethodReplacementService methodReplacement = new MethodReplacementService();
        JavaCodeParser extractor = new JavaCodeParser();
        GPTApiClient gptApiClient = new GPTApiClient();

        String errorOutput = checkedFileError(targetFile);
        if (errorOutput.isEmpty()) {
            LOGGER.info("No errors found in the file.");
            return false;
        }

        LOGGER.warn("Errors found in the file:" + System.lineSeparator() + errorOutput);

        while (!errorOutput.isEmpty()) {

            String methodWithError = String.valueOf(extractor.extractFirstClassFromFile(targetFile));

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
                return false;
            }

            LOGGER.info("Code block extracted from GPT response:" + System.lineSeparator() + codeBlock);

            boolean isMethodReplaced = methodReplacement.replaceMethodInFile(targetFile, codeBlock);

            if (!isMethodReplaced) {
                LOGGER.error("Failed to write code to file.");
                return false;
            }

            LOGGER.info("File written successfully. Recompiling with Checker Framework to check for additional warnings...");

            // This will be checked at the start of the next iteration
            errorOutput = checkedFileError(targetFile);

            if (!errorOutput.isEmpty()) {
                LOGGER.warn("Additional error(s) found after recompiling:" + System.lineSeparator() + errorOutput);
            }
        }

        LOGGER.info("No more errors found in the file.");
        return true;
    }

    /**
     * Minimizes a specific method in the target Java file using the SPECIMIN tool.
     *
     * @param outputDirectory Directory to store the minimized file.
     * @param root            Root directory of the target file.
     * @param targetFile      Path to the target Java file.
     * @param targetMethod    Method within the target file to minimize.
     * @return true if the minimization was successful; false otherwise.
     */
    public boolean minimizeTargetFile(String outputDirectory, String root, String targetFile, String targetMethod) {
        LOGGER.info("Minimizing source file...");
        SpeciminTool speciminTool = new SpeciminTool();
        boolean didSpeciminRun = speciminTool.runSpeciminTool(outputDirectory, root, targetFile, targetMethod);
        if (!didSpeciminRun) {
            LOGGER.error("Specimin tool failed to run.");
            return false;
        }
        LOGGER.info("Target file minimized successfully.");
        return true;
    }

    /**
     * Checks the target Java file for errors using the Checker Framework Compiler.
     *
     * @param targetFile Path to the Java file to check.
     * @return A string detailing detected errors, or an empty string if none were found.
     */
    public String checkedFileError(String targetFile) {
        CheckerFrameworkCompiler checkerFrameworkCompiler = new CheckerFrameworkCompiler();
        String errorOutput;

        try {
            errorOutput = checkerFrameworkCompiler.compileWithCheckerFramework(targetFile);
            return errorOutput;
        } catch (IOException e) {
            LOGGER.error("An IO error occurred while trying to compile the file: " + targetFile, e);
            // Return an empty string to indicate that
            // no errors were found in the checked file
            return "";
        }
    }
}
