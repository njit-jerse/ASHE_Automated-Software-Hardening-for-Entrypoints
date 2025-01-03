package edu.njit.jerse.automation;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import edu.njit.jerse.ashe.Ashe;
import edu.njit.jerse.ashe.llm.openai.models.GptModel;
import edu.njit.jerse.ashe.utils.JavaCodeCorrector;
import edu.njit.jerse.ashe.utils.ModelValidator;
import edu.njit.jerse.config.Configuration;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The {@code AsheAutomation} class applies {@link Ashe}'s minimization and correction mechanisms to
 * Java files in or under a given directory.
 */
public class AsheAutomation {
  private static final Logger LOGGER = LogManager.getLogger(AsheAutomation.class);

  // A standard Maven/Gradle project structure
  public static final String JAVA_SOURCE_DIR = "src/main/java";

  // Set java parser version to 21. TODO: find a better place for this
  static {
    StaticJavaParser.getParserConfiguration()
        .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
  }

  /**
   * Calls the {@link #processSingleJavaFile(Path, String, String)} method on each Java file in or
   * under the given directory.
   *
   * @param dirPath the path to the directory to iterate over
   * @param rootPath the root path of the project, used for determining the relative path of each
   *     Java file within the project structure
   * @param model the model to use for error correction
   * @throws IOException if an I/O error occurs while accessing the directory Multithreading notes
   *     If ASHE is running in dry run mode it will use all available cores/threads and copy the
   *     input to a temporary folder to prevent specimin from "cleaning up" files that another
   *     thread needs. If not performing a dry run a single thread will be used.
   */
  public static void processAllJavaFiles(Path dirPath, String rootPath, String model)
      throws IOException {
    LOGGER.info("Iterating over Java files in {}", dirPath);
    int threads = 1;
    if (model.equals(ModelValidator.DRY_RUN)) {
      threads = Runtime.getRuntime().availableProcessors();
      LOGGER.info("Dry run with {} threads", threads);
    }
    ForkJoinPool filePool = new ForkJoinPool(threads);
    try (Stream<Path> paths = Files.walk(dirPath)) {
      filePool
          .submit(
              () -> {
                paths
                    .filter(Files::isRegularFile)
                    // include only files with .java extension
                    .filter(
                        path ->
                            path.getFileName() != null
                                && path.getFileName().toString().endsWith(".java"))
                    .parallel()
                    .forEach(
                        path -> {
                          try {
                            // skip copying files if not dry run mode
                            if (model.equals(ModelValidator.DRY_RUN)) {
                              Path copiedPath =
                                  Files.createTempDirectory(
                                      "ASHE_THREAD_" + Thread.currentThread().getId() + "_");
                              FileUtils.copyDirectory(
                                  Paths.get(rootPath).toFile(), copiedPath.toFile());
                              Path pathWithPrefix =
                                  Paths.get(
                                      path.toString().replace(rootPath, copiedPath.toString()));
                              processSingleJavaFile(pathWithPrefix, copiedPath.toString(), model);
                            } else {
                              processSingleJavaFile(path, rootPath, model);
                            }
                          } catch (IOException
                              | ExecutionException
                              | InterruptedException
                              | TimeoutException e) {
                            String errorMessage = "Error processing Java file: " + path;
                            LOGGER.error(errorMessage, e);
                            throw new RuntimeException(errorMessage, e);
                          }
                        });
              })
          .join();
    }
    LOGGER.info("Completed iterating over Java files in {}", dirPath);
  }

  /**
   * Processes a Java file using {@link Ashe}. For each public method in a public class, {@link
   * Ashe#run(String, String, String, String)} is invoked to apply minimization and error
   * correction.
   *
   * @param javaFilePath the path to the Java file to be processed
   * @param projectRootPath the root path of the project, must be a prefix of the javaFile's
   *     absolute path
   * @param model the model to use for error correction
   * @throws IOException if an I/O error occurs when opening or parsing the file
   * @throws ExecutionException if {@link Ashe} encounters an error during execution
   * @throws InterruptedException if {@link Ashe}'s execution is interrupted
   * @throws TimeoutException if {@link Ashe}'s execution takes longer than the allowed time
   */
  private static void processSingleJavaFile(Path javaFilePath, String projectRootPath, String model)
      throws IOException, ExecutionException, InterruptedException, TimeoutException {
    String javaFileAbsolutePath = javaFilePath.toAbsolutePath().toString();

    LOGGER.info("Processing Java file: {}", javaFileAbsolutePath);
    LOGGER.info("Project root path: {}", projectRootPath);

    if (!javaFileAbsolutePath.startsWith(projectRootPath)) {
      String errorMessage =
          String.format(
              "The project root path %s must be a prefix of the Java file's absolute path %s",
              projectRootPath, javaFileAbsolutePath);
      LOGGER.error(errorMessage);
      throw new IllegalArgumentException(errorMessage);
    }

    CompilationUnit cu;
    try {
      cu = StaticJavaParser.parse(javaFilePath);
    } catch (IOException | ParseProblemException e) {
      LOGGER.error("Error parsing Java file: " + javaFileAbsolutePath, e);
      LOGGER.info("Skipping...");
      return;
    }

    // targetFile - the Java file ASHE will target for minimization and error correction
    // Example: edu/njit/jerse/automation/AsheAutomation.java
    String targetFile = formatRelativePathForJavaFile(javaFilePath, projectRootPath);

    for (TypeDeclaration<?> type : cu.findAll(TypeDeclaration.class)) {
      if (type.isPublic()) {
        for (BodyDeclaration<?> member : type.getMembers()) {
          if (member instanceof MethodDeclaration method) {
            if (method.isPublic() && method.getBody().isPresent()) {
              // targetMethod - the method ASHE will target for minimization and error correction
              // Example: edu.njit.jerse.automation.AsheAutomation#main(String[])
              String targetMethod = JavaCodeCorrector.fullyQualifiedMethodReference(method);
              LOGGER.info(targetMethod);
              Ashe.run(projectRootPath, targetFile, targetMethod, model);
            }
          }
        }
      }
    }
    LOGGER.info("Completed processing Java file: {}", javaFileAbsolutePath);
  }

  /**
   * Finds the relative path from the project root to the specified Java file. It formats the
   * relative path to use forward slashes and strips out the leading source directory path.
   *
   * @param javaFilePath the file path for which the relative path is needed Example:
   *     /absolute/path/src/main/java/com/example/foo/Bar.java
   * @param projectRootPath the absolute path to the root of the project. This path must be a prefix
   *     of the javaFile's absolute path. Example: /absolute/path/src/main/java
   * @return the relative path from the project root to the Java file Example of a relative path:
   *     com/example/foo/Bar.java
   */
  private static String formatRelativePathForJavaFile(Path javaFilePath, String projectRootPath) {
    Path projectRoot = Paths.get(projectRootPath);
    Path absoluteFilePath = javaFilePath.toAbsolutePath();

    String relativePath = projectRoot.relativize(absoluteFilePath).toString();
    relativePath = relativePath.replace(File.separatorChar, '/');

    // Expecting a standard Maven/Gradle project structure
    // I.E. src/main/java
    if (relativePath.startsWith(JAVA_SOURCE_DIR)) {
      relativePath = relativePath.substring(JAVA_SOURCE_DIR.length());
    }

    LOGGER.info("Formatted relative Java file path: {}", relativePath);
    return relativePath;
  }

  /**
   * The entry point of the application automating {@link Ashe}.
   *
   * @param args command-line arguments, expected order:
   *     <ol>
   *       <li>the absolute path to the directory containing Java files to be processed Example:
   *           /absolute/path/to/project/src/main/java/com/example/foo
   *       <li>the absolute root path of the project, which is used to determine the relative paths
   *           of Java files during processing. This path is used as a reference to calculate the
   *           relative paths for Java files and should be a common ancestor in the directory
   *           hierarchy for all Java files being processed. Example: /absolute/path/to/project
   *       <li>optional LLM argument:
   *           <ul>
   *             <li>"gpt-4" to run the {@link GptModel#GPT_4} model
   *             <li>"mock" to run the mock response defined in predefined_responses.txt
   *             <li>"dryrun" to run {@link Ashe#run} without a model, skipping the error correction
   *                 process
   *             <li>if this argument is omitted, a default model will be used ({@link
   *                 GptModel#GPT_4})
   *           </ul>
   *       <li>optional external props file
   *     </ol>
   *
   * @throws IOException if an I/O error occurs while accessing the directory
   */
  public static void main(String[] args) throws IOException {
    if (args.length < 2 || args.length > 4) {
      String errorMessage =
          String.format(
              "Invalid number of arguments: expected 2 or 3, but received %d. "
                  + "Required: 1) Directory Path, 2) Project Root Path. "
                  + "Optional: 3) Model name (LLM). "
                  + "Optional: 4) External properties file path, config.properties. "
                  + " Provided arguments: %s",
              args.length, Arrays.toString(args));

      LOGGER.error(errorMessage);
      throw new IllegalArgumentException(errorMessage);
    }

    String directoryPath = args[0];
    String projectRootPath = args[1];

    Path directory = Paths.get(directoryPath);
    if (!Files.exists(directory) || !Files.isDirectory(directory)) {
      LOGGER.error(
          "The specified directory does not exist or is not a directory: {}", directoryPath);
      System.exit(1);
    }

    // If no model is provided, use the default model.
    if (args.length == 2) {
      processAllJavaFiles(directory, projectRootPath, Ashe.MODEL);
      return;
    }

    Ashe.MODEL = args[2];
    ModelValidator.validateModel(Ashe.MODEL);

    if (args.length == 4) {
      String configPath = args[3];
      Configuration.getInstance(configPath);
    }

    processAllJavaFiles(directory, projectRootPath, Ashe.MODEL);
  }
}
