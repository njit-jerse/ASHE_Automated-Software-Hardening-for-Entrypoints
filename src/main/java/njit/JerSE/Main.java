package njit.JerSE;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * The main entry point for the application.
 * This class handles the initiation of the GPTPrototype to fix Java code.
 */
public class Main {
    private static final Logger LOGGER = LogManager.getLogger(Main.class);

    /**
     * The main method that initializes and runs the GPTPrototype.
     * It expects a single argument which is the class path of the Java class to be checked.
     *
     * @param args command line arguments. The first and only argument should be the class path.
     * @throws IllegalArgumentException if no arguments are provided or if too many arguments are provided
     * @throws IOException              if there's an issue reading or writing files during the code checking and fixing process
     * @throws IllegalStateException    if the API response from the GPT is not as expected
     * @throws InterruptedException     if the API request is interrupted
     */
    public static void main(String[] args) throws IllegalArgumentException, IOException, IllegalStateException, InterruptedException, ExecutionException, TimeoutException {
        if (args.length == 0) {
            LOGGER.error("No arguments provided.");
            throw new IllegalArgumentException("No arguments provided.");
        }

        if (args.length > 1) {
            LOGGER.error("Too many arguments provided.");
            throw new IllegalArgumentException("Too many arguments provided.");
        }

        LOGGER.info("Running ASHE...");
        // TODO: When SPECIMIN is running properly,
        //  we must accept more arguments to run SPECIMIN tool
        // class path of the class to be checked
        String classPath = args[0];
        ASHE ashe = new ASHE();
        ashe.fixJavaCodeUsingGPT(classPath);
    }


    // TODO: Leave for SPECIMIN tool testing
    // TODO: Receive help SPECIMIN team to run SPECIMIN tool
//    public static void main(String[] args) {
//        SpeciminTool specimin = new SpeciminTool();
//        specimin.runSpeciminTool(args[0], args[1], args[2], args[3]);
//    }
}
