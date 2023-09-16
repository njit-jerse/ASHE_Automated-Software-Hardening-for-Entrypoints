package njit.JerSE.services;

import njit.JerSE.utils.Configuration;

import java.io.*;

/**
 * Provides functionality for compiling Java classes using the Checker Framework.
 * <p>
 * The Checker Framework enhances Java's type system to make it more powerful and expressive,
 * allowing for early error detection in programs. This service utilizes the framework to
 * compile Java classes and detect potential type errors.
 */
public class CheckerFrameworkCompiler {

    /**
     * Compiles a Java class using the Checker Framework.
     *
     * @param classPath the path to the Java class that needs to be compiled
     * @return a string containing any errors produced during the compilation
     * @throws IOException If there's an error in executing the compilation command or reading its output
     */
    public String compileWithCheckerFramework(String classPath) throws IOException {
        // Compilation command with Checker Framework
        String[] command = compileCheckedClassCommand(classPath);
        Process compileProcess = Runtime.getRuntime().exec(command);
        String errorOutput = captureStream(compileProcess.getErrorStream());
        return extractErrors(errorOutput);
    }

    /**
     * Captures the content of an input stream into a string.
     *
     * @param stream the input stream to capture
     * @return a string representation of the stream's content
     * @throws IOException If there's an error in reading from the stream
     */
    private static String captureStream(InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line).append("\n");
        }
        return stringBuilder.toString();
    }

    /**
     * Extracts error messages from the provided error output string.
     * <p>
     * Specifically looks for the pattern "error:" and extracts the message after that pattern.
     *
     * @param errorMessage the error string to extract messages from
     * @return extracted error message, or an empty string if the "error:" pattern isn't found
     */
    private String extractErrors(String errorMessage) {
        int errorIndex = errorMessage.indexOf("error:");
        if (errorIndex != -1) {
            return errorMessage.substring(errorIndex).trim();
        }
        return ""; // No "error:" pattern found in the errorMessage
    }

    /**
     * Constructs the compilation command for a Java class using the Checker Framework.
     *
     * @param checkedClassPath the path to the Java class to be compiled
     * @return an array of strings representing the compilation command
     */
    private String[] compileCheckedClassCommand(String checkedClassPath) {
        Configuration config = new Configuration();
        String checkerJar = config.getPropertyValue("checker.jar.file");
        String checkerClasspath = config.getPropertyValue("checker.classpath");
        String checkerCommands = config.getPropertyValue("checker.commands");

        return new String[]{
                "java",
                "-jar",
                checkerJar,
                "-cp",
                checkerClasspath,
                "-processor",
                checkerCommands,
                checkedClassPath
        };
    }
}
