package njit.JerSE;

import njit.JerSE.services.MethodReplacementService;
import njit.JerSE.services.SpeciminTool;
import njit.JerSE.utils.Configuration;
import njit.JerSE.utils.JavaCodeCorrector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * The {@code ASHE} class orchestrates the correction, minimization, and method
 * replacement processes of Java files, leveraging the SPECIMIN tool, Checker
 * Framework, and GPT-aided error correction to refine and enhance Java code.
 * <p>
 * The ASHE execution flow encompasses:
 * <ol>
 *     <li>
 *         Utilizing the SPECIMIN tool to minimize specified methods in a target Java file,
 *         storing the minimized class in a dedicated 'specimin' directory within the ASHE project.
 *     </li>
 *     <li>
 *         Compiling the minimized file using the Checker Framework to validate its correctness
 *         and identify potential errors.
 *     </li>
 *     <li>
 *         In the presence of errors, sending the problematic code and errors to the ChatGPT API
 *         for corrective suggestions.
 *     </li>
 *     <li>
 *         Extracting code from the ChatGPT API response and overwriting the minimized class
 *         with the suggested corrections.
 *     </li>
 *     <li>
 *         Recompiling the corrected, minimized file using the Checker Framework, repeating the
 *         GPT-guided error correction as needed until no further errors are identified.
 *     </li>
 *     <li>
 *         Once error-free, utilizing the corrected, minimized class to overwrite the original
 *         method that was minimized in the target file, preserving the original file with an
 *         optimized, minimized method.
 *     </li>
 * </ol>
 * ASHE thus provides a comprehensive utility to minimize, validate, and correct Java methods,
 * enhancing code quality through an automated, GPT-aided refinement process.
 *
 * <p>
 * Example usage:
 * <pre>
 *     ASHE ashe = new ASHE();
 *     ashe.run(rootPath, targetFilePath, targetMethodName);
 * </pre>
 * </p>
 */
public class ASHE {
    private static final Logger LOGGER = LogManager.getLogger(ASHE.class);

    Configuration config = Configuration.getInstance();
    private final String outputDirectory = config.getPropertyValue("specimin.output.directory");

    /**
     * Orchestrates the running of ASHE's functionality by first minimizing
     * the target file with the SPECIMIN tool, then correcting its errors using GPT,
     * and finally replacing the original method in the target file.
     *
     * @param root         the root path where the target file is located
     * @param targetFile   the Java file to be minimized, corrected, and modified
     * @param targetMethod the target method in the Java file
     * @throws IOException          if an I/O error occurs during file operations
     * @throws ExecutionException   if an exception was thrown during task execution
     * @throws InterruptedException if the current thread was interrupted while waiting
     * @throws TimeoutException     if a timeout was encountered during task execution
     */
    void run(String root, String targetFile, String targetMethod) throws IOException, ExecutionException, InterruptedException, TimeoutException {
        LOGGER.info("Running ASHE...");

        JavaCodeCorrector corrector = new JavaCodeCorrector();
        MethodReplacementService methodReplacement = new MethodReplacementService();
        SpeciminTool specimin = new SpeciminTool();

        boolean didTargetFileMinimize = corrector.minimizeTargetFile(outputDirectory, root, targetFile, targetMethod);
        if (!didTargetFileMinimize) {
            LOGGER.error("Target file failed to minimize.");
            throw new RuntimeException("Target file failed to minimize.");
        }

        String sourceFilePath = outputDirectory + "/" + targetFile;

        boolean errorsReplacedInTargetFile = corrector.fixTargetFileErrorsWithGPT(sourceFilePath);
        if (!errorsReplacedInTargetFile) {
            if(corrector.checkedFileError(sourceFilePath).isEmpty()){
                LOGGER.info("No errors found in the file, no replacements needed.");
            } else {
                LOGGER.error("Errors were found but not replaced with GPT response.");
                throw new RuntimeException("Errors were not replaced with GPT response.");
            }
        }
        LOGGER.info("Errors replaced with GPT response successfully.");

        String originalFilePath = root + "/" + targetFile;
        boolean isOriginalMethodReplaced = methodReplacement.replaceOriginalTargetMethod(sourceFilePath, originalFilePath);

        if (!isOriginalMethodReplaced) {
            LOGGER.error("Original method was not replaced.");
            throw new RuntimeException("Original method was not replaced.");
        }
        LOGGER.info("Original method replaced successfully.");

        boolean minimizedDirectoryDeleted = specimin.removeMinimizedDirectory(outputDirectory, targetFile);
        if (!minimizedDirectoryDeleted) {
            LOGGER.error("Minimized directory was not deleted.");
            throw new RuntimeException("Minimized directory was not deleted.");
        }
        LOGGER.info("Minimized directory deleted successfully.");

        LOGGER.info("Exiting...");
    }

    /**
     * Entry point of the ASHE application. It expects three command-line arguments,
     * which are used to initiate the run of ASHE functionality. The arguments specify
     * the root path, target Java file, and target method to be processed. These
     * arguments are necessary to utilize the specified minimization with SEPCIMIN.
     *
     * @param args command-line arguments, expected order:
     * <ol>
     *     <li>
     *         root path of the target Java file
     *     </li>
     *     <li>
     *         name of the target Java file
     *     </li>
     *     <li>
     *         name of the target method within the Java file
     *     </li>
     * </ol>
     * @throws IOException          if an I/O error occurs during file operations
     * @throws ExecutionException   if an exception was thrown during task execution
     * @throws InterruptedException if the current thread was interrupted while waiting
     * @throws TimeoutException     if a timeout was encountered during task execution
     */
    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException, TimeoutException {
        if (args.length != 3) {
            LOGGER.error("Invalid number of arguments provided.");
            throw new IllegalArgumentException("Invalid number of arguments provided.");
        }

        // SPECIMIN arguments
        String root = args[0];
        String targetFile = args[1];
        String targetMethod = args[2];

        ASHE ashe = new ASHE();
        ashe.run(root, targetFile, targetMethod);
    }
}