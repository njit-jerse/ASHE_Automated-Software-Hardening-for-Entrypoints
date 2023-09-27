package njit.JerSE;

import java.io.IOException;


// TODO: Throughout the entire project, change System.out.println() to Logger
/**
 * The main entry point for the application.
 * This class handles the initiation of the GPTPrototype to fix Java code.
 */
public class Main {

    /**
     * The main method that initializes and runs the GPTPrototype.
     * It expects a single argument which is the class path of the Java class to be checked.
     *
     * @param args command line arguments. The first and only argument should be the class path.
     * @throws IllegalArgumentException if no arguments are provided or if too many arguments are provided
     * @throws IOException if there's an issue reading or writing files during the code checking and fixing process
     * @throws IllegalStateException if the API response from the GPT is not as expected
     * @throws InterruptedException if the API request is interrupted
     */
    public static void main(String[] args) throws IllegalArgumentException, IOException, IllegalStateException, InterruptedException  {
        if (args.length == 0) {
            throw new IllegalArgumentException("No arguments provided.");
        }

        if (args.length > 1) {
            throw new IllegalArgumentException("Too many arguments provided.");
        }

        System.out.println("Running ASHE...");
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
//        specimin.runSpeciminTool();
//    }
}
