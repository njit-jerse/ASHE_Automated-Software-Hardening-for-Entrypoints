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
import java.util.List;

// TODO: Add this functionality to the README
/**
 * The {@code RepositoryAutomationEngine} class automates the process of cloning or fetching repositories
 * listed in a CSV file and then processing each repository. Once a repository is cloned or fetched and
 * checked out to the specified branch, the repository is scanned for Java project roots. For each Java
 * project root found, the {@code AsheAutomation} process is applied to automate {@code ASHE} on the project.
 */
public class RepositoryAutomationEngine {

    private static final Logger LOGGER = LogManager.getLogger(RepositoryAutomationEngine.class);

    /**
     * A record to store repository information.
     */
    private record Repository(
            /**
             * The URL of the repository.
             */
            String url,
            /**
             * The branch of the repository to be cloned or fetched.
             */
            String branch
    ) {
    }

    /**
     * Reads and processes repositories listed in a specified CSV file. This method
     * takes each entry in the CSV file, clones or fetches the corresponding repository,
     * and then processes it by scanning for Java project roots. Upon locating a Java project
     * root, the method delegates to {@code AsheAutomation} to process the Java files within.
     * <p>
     * The format of the CSV file is as follows:
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
     * @param csvFilePath path to the CSV file containing repository URLs and branch names.
     *                    Each line in the CSV file should correspond to a repository,
     *                    with 'Repository' and 'Branch' columns as specified above.
     * @param repoDir     directory where the repositories will be cloned or fetched
     */
    private static void readAndProcessRepositoriesCsv(String csvFilePath, String repoDir) {
        LOGGER.info("Starting to read and process CSV file: {}", csvFilePath);
        try {
            List<Repository> repositories = readRepositoriesFromCsv(csvFilePath);
            processAllRepositories(repositories, repoDir);
        } catch (Exception e) {
            LOGGER.error("Error processing CSV file: {}", csvFilePath, e);
            throw new RuntimeException("Error processing CSV file: " + csvFilePath, e);
        }
        LOGGER.info("Completed processing CSV file: {}", csvFilePath);
    }

    /**
     * Reads a CSV file containing repository information and converts it into a list of
     * {@code Repository} records. The CSV file must have a header specifying these columns.
     * This method sets up the CSV format to skip the header record during parsing.
     *
     * @param csvFilePath the file path to the CSV file that contains the repository information
     * @return a list of maps where each map contains the 'Repository' URL and the 'Branch' name
     * @throws RuntimeException if an error occurs while trying to read or parse the CSV file. The original
     *                          IOException is encapsulated within this RuntimeException.
     */
    private static List<Repository> readRepositoriesFromCsv(String csvFilePath) {
        LOGGER.info("Reading CSV file: {}", csvFilePath);
        List<Repository> repositories = new ArrayList<>();

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build();

        try (FileReader reader = new FileReader(csvFilePath);
             CSVParser csvParser = new CSVParser(reader, csvFormat)) {

            for (CSVRecord csvRecord : csvParser) {
                Repository repoInfo = new Repository(csvRecord.get("Repository"), csvRecord.get("Branch"));
                repositories.add(repoInfo);
                LOGGER.info("Added repository: {}", repoInfo);
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
     * @param repositories a list of {@code Repository} records, each containing the 'Repository' URL and the
     *                     'Branch' name
     * @param repoDir      the directory where the repositories will be cloned or fetched
     * @throws GitAPIException if a Git-specific error occurs during the cloning or fetching of the repositories
     * @throws IOException     if an I/O error occurs during the access or manipulation of the repository files
     */
    private static void processAllRepositories(List<Repository> repositories, String repoDir)
            throws GitAPIException, IOException {
        LOGGER.info("Starting to process repositories");
        for (Repository repository : repositories) {
            String repoUrl = repository.url();
            String branch = repository.branch();

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
     * Clones a repository from the given URL or fetches updates if the repository
     * already exists locally.
     *
     * @param repoUrl URL of the repository to be cloned or fetched
     * @param branch  the branch to be checked out or updated
     * @param repoDir base directory for storing cloned repositories
     * @return path to the local repository
     * @throws GitAPIException if any error occurs during cloning or fetching the repository
     * @throws IOException     if any error occurs during the cloning or fetching of the repository
     */
    public static Path cloneOrFetchRepository(String repoUrl, String branch, String repoDir)
            throws GitAPIException, IOException {
        LOGGER.info("Attempting to clone or fetch repository: {}", repoUrl);

        String repoName = extractRepoName(repoUrl);
        Path repoPath = Paths.get(repoDir, repoName).normalize();
        if (Files.notExists(repoPath)) {
            GitUtils.cloneRepository(repoUrl, branch, repoPath.toFile());
        } else {
            GitUtils.fetchRepository(repoPath.toFile());
        }
        return repoPath;
    }

    /**
     * Processes a given repository by scanning for Java project roots. For each Java project found,
     * this method applies the {@code AsheAutomation} process. The method supports processing
     * multiple projects within a single repository.
     *
     * @param repoPath path to the local repository
     * @param branch   the branch of the repository to be processed
     * @throws IOException if an I/O error occurs while accessing the file system
     */
    private static void processSingleRepository(Path repoPath, String branch) throws IOException {
        LOGGER.info("Processing repository at: {} for branch: {}", repoPath, branch);

        List<File> projectRoots = ProjectRootFinder.findJavaRoots(repoPath.toFile());
        for (File projectRoot : projectRoots) {
            LOGGER.info("Processing project root: " + projectRoot.getPath());
            AsheAutomation.iterateJavaFiles(projectRoot, projectRoot.getAbsolutePath());
        }

        LOGGER.info("Completed processing for branch: {} in repository path: {}", branch, repoPath);
    }

    /**
     * Extracts the repository name from its URL.
     *
     * @param repoUrl URL of the repository
     * @return name of the repository extracted from the URL
     * @throws IllegalArgumentException if the repository URL is invalid or the name cannot be extracted
     */
    private static String extractRepoName(String repoUrl) {
        LOGGER.info("Extracting repository name from URL: {}", repoUrl);

        int lastSlashIndex = repoUrl.lastIndexOf('/');
        int gitIndex = repoUrl.lastIndexOf(".git");

        // Checks if ".git" is present in the URL and not at the end
        boolean hasGitExtension = gitIndex > 0;

        // Checks if there are characters after the last slash
        boolean hasCharsAfterLastSlash = lastSlashIndex < repoUrl.length() - 1;

        // Has '.git' extension. Example: https://github.com/jonathan-m-phillips/ASHE_Automated-Software-Hardening-for-Entrypoints.git
        if (hasGitExtension && hasCharsAfterLastSlash) {
            return repoUrl.substring(lastSlashIndex + 1, gitIndex);
        }

        // No '.git' extension, but a valid URL. Example: https://github.com/jonathan-m-phillips/ASHE_Automated-Software-Hardening-for-Entrypoints
        if (hasCharsAfterLastSlash) {
            return repoUrl.substring(lastSlashIndex + 1);
        }

        throw new IllegalArgumentException("Invalid repository URL");
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
            LOGGER.error("Please provide the path to the repositories CSV file and the directory where you would" +
                    "like to clone repositories.");
            System.exit(1);
        }

        String csvFilePath = args[0];
        String repoDir = args[1];
        readAndProcessRepositoriesCsv(csvFilePath, repoDir);
    }
}