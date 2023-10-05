package njit.JerSE.services;

import njit.JerSE.utils.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * A utility class to manage and run SPECIMIN - a specification minimizer tool.
 * <p>
 * SPECIMIN is designed to minimize Java classes based on specific methods,
 * reducing them to a simplified form where only the specified method is present.
 * <p>
 * The SpeciminTool class interfaces with SPECIMIN, providing functionalities to:
 * <ul>
 *     <li>Run the SPECIMIN tool with specified parameters.</li>
 *     <li>Modify the package declaration of the minimized Java file.</li>
 *     <li>Delete minimized directories if needed.</li>
 * </ul>
 */
public class SpeciminTool {
    private static final Logger LOGGER = LogManager.getLogger(SpeciminTool.class);

    /**
     * Executes and manages SPECIMIN using specified paths and targets.
     * <p>
     *
     * @param outputDirectory Path where the output will be written.
     * @param root            The root directory for the tool.
     * @param targetFile      File to be targeted by the tool.
     * @param targetMethod    Method to be targeted by the tool.
     * @return true if the operation was successful, false otherwise.
     */
    public boolean runSpeciminTool(String outputDirectory, String root, String targetFile, String targetMethod) {
        LOGGER.info("Running SpeciminTool...");

        Configuration config = Configuration.getInstance();
        String speciminPath = config.getPropertyValue("specimin.tool.path");

        try {
            // Formatting command-line arguments for SPECIMIN execution
            String myArgs = String.format("--outputDirectory \"%s\" --root \"%s\" --targetFile \"%s\" --targetMethod \"%s\"",
                    outputDirectory, root, targetFile, targetMethod);
            String argsWithOption = "--args=" + myArgs;

            // Preparing commands list for ProcessBuilder
            List<String> commands = new ArrayList<>();
            commands.add(speciminPath + "/gradlew");
            commands.add("run");
            commands.add(argsWithOption);
            System.out.println("Executing command:");
            for (String command : commands) {
                System.out.println(command);
            }

            // Setting up process builder to run commands
            ProcessBuilder builder = new ProcessBuilder(commands);
            builder.redirectErrorStream(true); // This merges the error and output streams
            builder.directory(new File(speciminPath));

            Process process = builder.start();

            // Logging the output of the process
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOGGER.info(line);
                }
            }

            // Waiting for the process to finish and logging the exit value in case of errors
            int exitValue = process.waitFor();
            if (exitValue != 0) {
                LOGGER.error("Error executing the command. Exit value: " + exitValue);
            }

        } catch (IOException | InterruptedException e) {
            LOGGER.error("Exception during SPECIMIN execution", e);
            return false;
        }

        // Modifying the package declaration in the generated file and writing it back
        return modifyAndWritePackageDeclaration(outputDirectory, targetFile);
    }

    /**
     * Deletes the specified directory where the minimized file was held,
     * as well as its contents from the file system.
     *
     * <p>Note: This method will delete the directory even if it contains
     * a .gitkeep file, which might affect version control if changes are committed.
     *
     * @param parentDirectory The path to the parent directory.
     * @param targetFile The path to the minimized file to be deleted.
     * @return true if the directory was deleted successfully; false otherwise.
     */
    public boolean removeMinimizedDirectory(String parentDirectory, String targetFile) {
        // Extracting the subdirectory name from the target file path
        String subDirectory = extractFirstSubDirectory(targetFile);
        Path path = Paths.get(parentDirectory, subDirectory);

        // Check if the path exists and is a directory
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            LOGGER.error("Directory does not exist: " + path);
            return false;
        }

        try {
            // Walking through the directory tree and deleting files and directories
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                // Deleting each file encountered
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                // Deleting directories after all their contents have been visited and deleted
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOGGER.error("Error during directory deletion: ", e);
            return false;
        }

        LOGGER.info("Directory deleted: " + path);
        return true;
    }

    /**
     * Modifies and writes the package declaration of a Java file.
     *
     * <p>This method reads a specified Java file's content, modifies its package
     * declaration, then writes the modified content back into the file. If an
     * I/O error occurs, it logs the error and returns {@code false}.
     *
     * @param outputDirectory The directory path where the target file is located.
     * @param targetFile The name of the file to be modified.
     * @return {@code true} if the file is successfully modified and written, {@code false} otherwise.
     */
    private boolean modifyAndWritePackageDeclaration(String outputDirectory, String targetFile) {
        try {
            // Constructing the path to the output file created by SPECIMIN
            Path speciminOutputDir = Paths.get(outputDirectory, targetFile);
            String fileContent = Files.readString(speciminOutputDir);

            // Modifying the package declaration within the file content
            String modifiedPackage = modifyPackageDeclaration(fileContent);

            // Writing the modified content back to the file, overwriting the original content
            Files.writeString(speciminOutputDir, modifiedPackage);

            LOGGER.info("File package modified and written to: " + speciminOutputDir);
            return true;
        } catch (IOException e) {
            LOGGER.error("Error during file modification: ", e);
            return false;
        }
    }

    /**
     * Extracts the first subdirectory from the given file path.
     *
     * @param filePath The full file path.
     * @return The first subdirectory name, or an empty string if none exists.
     */
    private String extractFirstSubDirectory(String filePath) {
        // Check if the filePath is valid and contains at least one '/'
        if (filePath == null || !filePath.contains("/")) {
            return "";
        }

        // Extract the first subdirectory
        return filePath.substring(0, filePath.indexOf('/'));
    }

    /**
     * Modifies the package declaration in Java file content.
     * <p>
     * This method prepends "specimin." to the existing package name in the provided
     * Java file content string, effectively moving the class to a "specimin" subpackage.
     *
     * @param fileContent Original Java file content
     * @return Modified Java file content with adjusted package name
     */
    private String modifyPackageDeclaration(String fileContent) {
        // Regular expression to match package declarations in Java source code
        String regex = "package\\s+([a-zA-Z_][.\\w]*);";
        // Replacement string, adding specimin to the package declaration
        String replacement = "package specimin.$1;";

        // Replace the package declaration in the file
        return fileContent.replaceAll(regex, replacement);
    }
}
