package njit.JerSE.services;

import njit.JerSE.utils.Configuration;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Note: This class is still under development and does not run properly.
 */
public class SpeciminTool {

    /**
     * Note: This method is still under development and does not run properly.
     */
    public void runSpeciminTool() {
        System.out.println("Running SpeciminTool...");
        Configuration config = Configuration.getInstance();
        String outputDirectory = "this path would be the same as the targetRoot";
        String speciminPath = config.getPropertyValue("specimin.tool.path");
        String targetRoot = "target root would be provided in the arguments of main";
        String targetFile = "target file would be provided in the arguments of main";
        String targetMethod = "target method would be provided in the arguments of main";
        try {
            String myArgs = String.format("--outputDirectory \"%s\" --root \"%s\" --targetFile \"%s\" --targetMethod \"%s\"",
                    outputDirectory, targetRoot, targetFile, targetMethod);
            String argsWithOption = "--args=" + myArgs;

            ProcessBuilder builder = new ProcessBuilder(
                    speciminPath + "/gradlew",
                    "run",
                    argsWithOption
            );


            List<String> commands = builder.command();
            System.out.println("Executing command:");
            for (String command : commands) {
                System.out.println(command);
            }

            builder.redirectErrorStream(true); // This merges the error and output streams
            builder.directory(new File(speciminPath));

            Process process = builder.start();

            // Read the output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            // Wait for the process to finish and check the exit value
            int exitValue = process.waitFor();
            if (exitValue != 0) {
                System.out.println("There was an error executing the command. Exit value: " + exitValue);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
