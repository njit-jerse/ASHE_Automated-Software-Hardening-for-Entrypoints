package edu.njit.jerse.automation;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import edu.njit.jerse.ashe.ASHE;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

// TODO: Add this functionality to the README
/**
 * The {@code ASHEAutomation} class provides automation for processing Java files
 * within a given directory, specifically aiming to process each Java file using
 * the ASHE framework. It iterates over all files in a directory, identifies Java files,
 * and then processes each Java file to apply ASHE's minimization and correction mechanisms.
 * <p>
 * This class is primarily utilized for batch processing of multiple Java files within
 * a project, applying ASHE's methodologies to each file, thereby aiding in code
 * optimization and error correction at a larger scale.
 */
public class ASHEAutomation {
    private static final Logger LOGGER = LogManager.getLogger(ASHEAutomation.class);

    /**
     * Iterates over all files in the given directory. If a file is a directory itself,
     * this method is called recursively on that directory. If a file is a Java file,
     * it is processed using the {@code processJavaFile} method.
     *
     * @param dir      The directory to iterate over.
     * @param rootPath The root path of the project, used for determining the relative
     *                 path of each Java file within the project structure.
     * @throws IOException          if an I/O error occurs while accessing the directory.
     * @throws ExecutionException   if an execution error occurs while processing a Java file.
     * @throws InterruptedException if the thread running the process is interrupted.
     * @throws TimeoutException     if the processing of a Java file exceeds a defined timeout.
     */
    public static void iterateJavaFiles(File dir, String rootPath)
            throws IOException, ExecutionException, InterruptedException, TimeoutException {
        LOGGER.info("Iterating over Java files in directory: {}", dir.getAbsolutePath());
        File[] files = dir.listFiles();

        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                iterateJavaFiles(file, rootPath);
            } else if (file.getName().endsWith(".java")) {
                processJavaFile(file, rootPath);
            }
        }
    }

    /**
     * Processes an individual Java file using the ASHE framework. This involves parsing
     * the file to extract class and method information, and then for each public method,
     * the ASHE 'run' method is invoked to apply minimization and error correction.
     *
     * @param javaFile        The Java file to be processed.
     * @param projectRootPath The root path of the project, used for determining the
     *                        relative path of the Java file.
     * @throws IOException          if an I/O error occurs when opening or parsing the file.
     * @throws ExecutionException   if the ASHE framework encounters an error during execution.
     * @throws InterruptedException if the ASHE execution is interrupted.
     * @throws TimeoutException     if the ASHE execution takes longer than the allowed time.
     */
    private static void processJavaFile(File javaFile, String projectRootPath)
            throws IOException, ExecutionException, InterruptedException, TimeoutException {
        LOGGER.info("Processing Java file: {}", javaFile.getAbsolutePath());
        CompilationUnit cu = StaticJavaParser.parse(javaFile);

        String targetFile = javaFile
                .getAbsolutePath()
                .substring(projectRootPath.length() + 1)
                .replace(File.separatorChar, '/');

        if (targetFile.startsWith("src/main/java/")) {
            targetFile = targetFile.substring("src/main/java/".length());
        }

        String packageName = cu.getPackageDeclaration().map(NodeWithName::getNameAsString).orElse("");
        for (TypeDeclaration<?> type : cu.getTypes()) {
            if (type.isPublic()) {
                String fullClassName = packageName.isEmpty() ? type.getNameAsString() : packageName + "." + type.getNameAsString();
                for (BodyDeclaration<?> member : type.getMembers()) {
                    if (member instanceof MethodDeclaration method) {
                        if (method.isPublic()) {
                            String parameters = method.getParameters().stream()
                                    .map(p -> p.getType().asString())
                                    .collect(Collectors.joining(", "));
                            String targetMethod = fullClassName + "#" + method.getNameAsString() + "(" + parameters + ")";

                            ASHE ashe = new ASHE();
                            ashe.run(projectRootPath, targetFile, targetMethod);
                        }
                    }
                }
            }
        }
        LOGGER.info("Completed processing Java file: {}", javaFile.getAbsolutePath());
    }

    /**
     * The entry point of the application automating ASHE.
     * Expects two command-line arguments:
     * the path to the directory containing Java files and the root path of the project.
     * This method initiates the automation process on the specified directory, applying
     * the ASHE framework to each Java file found.
     *
     * @param args command-line arguments, expected order:
     *             <ol>
     *                 <li>The path to the directory containing Java files to be processed.</li>
     *                 <li>The root path of the project for relative path calculation.</li>
     *             </ol>
     * @throws IOException          if an I/O error occurs while accessing the directory.
     * @throws ExecutionException   if an execution error occurs during the processing.
     * @throws InterruptedException if the thread running the process is interrupted.
     * @throws TimeoutException     if the processing of a file exceeds the allotted time.
     */
    public static void main(String[] args)
            throws IOException, ExecutionException, InterruptedException, TimeoutException {
        if (args.length != 2) {
            LOGGER.error("Invalid number of arguments. Expected 2 arguments: <Directory Path> <Project Root Path>");
            System.exit(1);
        }

        String directoryPath = args[0];
        String projectRootPath = args[1];

        File directory = new File(directoryPath);
        if (!directory.exists() || !directory.isDirectory()) {
            LOGGER.error("The specified directory does not exist or is not a directory: {}", directoryPath);
            System.exit(1);
        }

        iterateJavaFiles(directory, projectRootPath);
    }
}