package edu.njit.jerse.automation;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

// TODO: Add this functionality to the README
/**
 * The {@code RepositoryAutomationEngine} class automates the process of cloning or fetching
 * repositories listed in a CSV file and then processing each repository. It is designed to
 * work with repositories containing Java projects, applying specific automation routines
 * such as ASHEAutomation.
 * <p>
 * This class provides a comprehensive solution for batch processing multiple repositories
 * by automating the tasks of repository cloning, fetching, and Java file processing.
 * </p>
 */
public class RepositoryAutomationEngine {

    private static final Logger LOGGER = LogManager.getLogger(RepositoryAutomationEngine.class);

    /**
     * Reads and processes repositories listed in a specified CSV file. This method
     * takes each entry in the CSV file, clones or fetches the corresponding repository,
     * and then processes it by scanning for Java project roots. It applies predefined
     * automation routines to each found project.
     * <p>
     * The expected format of the CSV file is as follows:
     * <pre>
     *     Repository, Branch
     *     https://url-to-your-repository.git, your-branch-to-clone
     * </pre>
     * Each line in the CSV should contain two columns:
     * <ul>
     *     <li><b>Repository:</b> The URL of the Git repository (ending in '.git')</li>
     *     <li><b>Branch:</b> The name of the branch in the repository to be cloned or fetched</li>
     * </ul>
     * </p>
     *
     * @param csvFilePath Path to the CSV file containing repository URLs and branch names.
     *                    Each line in the CSV file should correspond to a repository,
     *                    with 'Repository' and 'Branch' columns as specified above.
     * @param repoDir     Directory where the repositories will be cloned or fetched.
     */
    private static void readAndProcessRepositoriesCsv(String csvFilePath, String repoDir) {
        LOGGER.info("Starting to read and process CSV file: {}", csvFilePath);
        try {
            List<Map<String, String>> repositories = readRepositoriesFromCsv(csvFilePath);
            processAllRepositories(repositories, repoDir);
        } catch (Exception e) {
            LOGGER.error("Error processing CSV file: {}", csvFilePath, e);
            throw new RuntimeException("Error processing CSV file: " + csvFilePath, e);
        }
        LOGGER.info("Completed processing CSV file: {}", csvFilePath);
    }

    /**
     * Reads a CSV file containing repository information and converts it into a list of maps.
     * Each map entry corresponds to a single repository's data, with keys 'Repository' for the URL
     * and 'Branch' for the branch name. The CSV file is expected to have a header specifying these columns.
     * This method sets up the CSV format to skip the header record during parsing.
     *
     * @param csvFilePath The file path to the CSV file that contains the repository information.
     * @return A list of maps where each map contains the 'Repository' URL and the 'Branch' name.
     * @throws RuntimeException if an error occurs while trying to read or parse the CSV file. The original
     *                          IOException is encapsulated within this RuntimeException.
     */
    private static List<Map<String, String>> readRepositoriesFromCsv(String csvFilePath) {
        LOGGER.info("Reading CSV file: {}", csvFilePath);
        List<Map<String, String>> repositories = new ArrayList<>();

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build();

        try (FileReader reader = new FileReader(csvFilePath);
             CSVParser csvParser = new CSVParser(reader, csvFormat)) {

            for (CSVRecord csvRecord : csvParser) {
                Map<String, String> repoInfo = new HashMap<>();
                repoInfo.put("Repository", csvRecord.get("Repository"));
                repoInfo.put("Branch", csvRecord.get("Branch"));
                repositories.add(repoInfo);
            }
        } catch (IOException e) {
            LOGGER.error("Error reading CSV file: {}", csvFilePath, e);
            throw new RuntimeException("Error reading CSV file: " + csvFilePath, e);
        }
        return repositories;
    }

    /**
     * Processes all repositories specified in the list. Each repository is cloned or fetched,
     * and then further processed to apply automation routines.
     *
     * @param repositories A list of maps, each containing the 'Repository' URL and the 'Branch' name.
     * @param repoDir      The directory where the repositories will be cloned or fetched.
     * @throws GitAPIException      if a Git-specific error occurs during the cloning or fetching of the repositories.
     * @throws IOException          if an I/O error occurs during the access or manipulation of the repository files.
     * @throws ExecutionException   if an error occurs during the concurrent execution of repository processing.
     * @throws InterruptedException if the processing of a repository is interrupted.
     * @throws TimeoutException     if the processing of a repository exceeds a given timeout constraint.
     */
    private static void processAllRepositories(List<Map<String, String>> repositories, String repoDir)
            throws GitAPIException, IOException, ExecutionException, InterruptedException, TimeoutException {
        LOGGER.info("Starting to process repositories");
        for (Map<String, String> repoInfo : repositories) {
            String repoUrl = repoInfo.get("Repository");
            String branch = repoInfo.get("Branch");

            if (repoUrl == null || branch == null) {
                LOGGER.error("Repository URL or branch is missing in the list.");
                throw new IllegalArgumentException("Repository URL or branch cannot be null.");
            }

            Path repoPath = cloneOrFetchRepository(repoUrl, branch, repoDir);
            processSingleRepository(repoPath, branch);
        }
        LOGGER.info("Completed processing repositories");
    }

    /**
     * Clones a new repository from the given URL or fetches updates if the repository
     * already exists locally. The method identifies the local path for the repository
     * and ensures up-to-date synchronization with the remote repository.
     *
     * @param repoUrl URL of the repository to be cloned or fetched.
     * @param branch  The branch to be checked out or updated.
     * @param repoDir Base directory for storing cloned repositories.
     * @return Path to the local repository.
     * @throws GitAPIException if any error occurs during cloning or fetching the repository.
     * @throws IOException     if any error occurs during the cloning or fetching of the repository.
     */
    public static Path cloneOrFetchRepository(String repoUrl, String branch, String repoDir)
            throws GitAPIException, IOException {
        LOGGER.info("Attempting to clone or fetch repository: {}", repoUrl);

        String repoName = extractRepoName(repoUrl);
        Path repoPath = Paths.get(repoDir, repoName).normalize();
        if (Files.notExists(repoPath)) {
            GitUtils.cloneRepository(repoUrl, branch, repoPath.toFile());
            LOGGER.info("Cloned new repository: " + repoUrl);
        } else {
            GitUtils.fetchRepository(repoPath.toFile());
        }
        return repoPath;
    }

    /**
     * Processes a given repository by scanning for Java project roots and applying
     * automation routines to each Java project found. The method supports processing
     * multiple projects within a single repository.
     *
     * @param repoPath Path to the local repository.
     * @param branch   The branch of the repository to be processed.
     * @throws IOException          if an I/O error occurs while accessing the file system.
     * @throws ExecutionException   if an error occurs during the execution of the automation routines.
     * @throws InterruptedException if the process is interrupted while waiting for the automation
     *                              routines to complete.
     * @throws TimeoutException     if the automation routines take longer than the expected time
     *                              to complete.
     */
    private static void processSingleRepository(Path repoPath, String branch)
            throws IOException, ExecutionException, InterruptedException, TimeoutException {
        LOGGER.info("Processing repository at: {} for branch: {}", repoPath, branch);

        List<File> projectRoots = ProjectRootScanner.findJavaRoots(repoPath.toFile());
        for (File projectRoot : projectRoots) {
            LOGGER.info("Processing project root: " + projectRoot.getPath());
            ASHEAutomation.iterateJavaFiles(projectRoot, projectRoot.getAbsolutePath());
        }

        LOGGER.info("Completed processing for branch: {} in repository path: {}", branch, repoPath);
    }

    /**
     * Extracts the repository name from its URL. The method assumes that the URL
     * ends with '.git' and extracts the name accordingly.
     *
     * @param repoUrl URL of the repository.
     * @return Name of the repository extracted from the URL.
     * @throws IllegalArgumentException if the repository URL is invalid or the name cannot be extracted.
     */
    private static String extractRepoName(String repoUrl) {
        LOGGER.info("Extracting repository name from URL: {}", repoUrl);

        // Extract the name of the repo from the URL
        // Assumes URL ends with '.git'
        int lastSlashIndex = repoUrl.lastIndexOf('/');
        int gitIndex = repoUrl.lastIndexOf(".git");
        if (gitIndex > 0 && lastSlashIndex < repoUrl.length() - 1) {
            return repoUrl.substring(lastSlashIndex + 1, gitIndex);
        } else if (lastSlashIndex < repoUrl.length() - 1) {
            return repoUrl.substring(lastSlashIndex + 1);
        } else {
            throw new IllegalArgumentException("Invalid repository URL");
        }
    }

    /**
     * The entry point of the application when trying to read and clone repositories from a CSV file.
     * Expects two command-line arguments: the path to the repositories CSV file and the directory where
     * the repositories will be cloned. The method initiates the process of reading and processing
     * repositories listed in the CSV file.
     *
     * @param args command-line arguments, expected order:
     *             <ol>
     *                 <li>Path to the repositories CSV file.</li>
     *                 <li>Directory for cloning repositories.</li>
     *             </ol>
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            LOGGER.error("Provide 2 arguments: the repositories CSV file and the directory in which to clone repositories.");
            System.exit(1);
        }

        String csvFilePath = args[0];
        String repoDir = args[1];
        readAndProcessRepositoriesCsv(csvFilePath, repoDir);
    }
}
