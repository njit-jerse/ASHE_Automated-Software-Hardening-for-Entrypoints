package edu.njit.jerse.automation;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

/**
 * The {@code RepositoryAutomationEngine} class clones or fetches repositories
 * listed in a CSV file.  The {@link AsheAutomation} process is run on
 * every Java file within the repositories.
 */
public class RepositoryAutomationEngine {

    private static final Logger LOGGER = LogManager.getLogger(RepositoryAutomationEngine.class);

    /**
     * A record to store repository information.
     */
    private record Repository(
            /** The URL of the repository. */
            String url,
            /** The branch of the repository to be cloned or fetched. Null means to use the default branch (often "main" or "master"). */
            @Nullable String branch
    ) {
        Repository {
            if (url == null || url.isEmpty()) {
                throw new IllegalArgumentException("Repository URL must not be null or empty.");
            }
        }
    }

    // TODO: Do we want to consider adding the LLM model in the CSV file?
    // TODO: Users would be able to specify the model to use for each repository.
    // TODO: This would be beneficial if we wanted to run multiple different models on the same repository.
    /**
     * Processes repositories listed in the given CSV file. This method
     * takes each entry in the CSV file, clones or fetches the corresponding repository,
     * scans it for Java project roots, and runs
     * {@link AsheAutomation} to process the Java files within each project.
     * <p>
     * The format of the CSV file is as follows:
     * <pre>
     *     Repository, Branch
     *     https://url-to-your-repository.git, your-branch-to-clone
     * </pre>
     * Each line in the CSV should contain exactly these two columns, in the given order:
     * <ul>
     *     <li><b>Repository:</b> The URL of the Git repository (ending in '.git')</li>
     *     <li><b>Branch:</b> The name of the branch in the repository to be cloned or fetched</li>
     * </ul>
     * </p>
     *
     * @param csvFilePath path to the CSV file containing repository URLs and branch names,
     *                    in the format specified above.
     * @param repoDir     directory where the repositories will be cloned or fetched
     */
    private static void readAndProcessRepositoriesCsv(String csvFilePath, String repoDir, String model) {
        LOGGER.info("Starting to process CSV file: {}", csvFilePath);
        try {
            List<Repository> repositories = readRepositoriesFromCsv(csvFilePath);
            processAllRepositories(repositories, repoDir, model);
        } catch (Exception e) {
            LOGGER.error("Error processing CSV file: {}", csvFilePath, e);
            throw new RuntimeException("Error processing CSV file: " + csvFilePath, e);
        }
        LOGGER.info("Completed processing CSV file: {}", csvFilePath);
    }

    /**
     * Reads a CSV file containing repository information and converts it into a list of
     * {@link Repository} records. The CSV file must have a header specifying the "Repository"
     * and "Branch" columns. This method sets up the CSV format to skip the header record
     * during parsing.
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
     * then individually processed with {@link #processSingleRepository(Path, String, String)}.
     *
     * @param repositories a list of {@code Repository} records
     * @param repoDir      the directory where the repositories will be cloned or fetched
     * @param model        the large language model to use
     * @throws GitAPIException if a Git-specific error occurs during the cloning or fetching of the repositories
     * @throws IOException     if an I/O error occurs during the access or manipulation of the repository files
     */
    private static void processAllRepositories(List<Repository> repositories, String repoDir, String model)
            throws GitAPIException, IOException {
        int numOfRepositories = repositories.size();
        LOGGER.info("Starting to process {} repositories", numOfRepositories);
        for (Repository repository : repositories) {
            String repoUrl = repository.url();
            String branch = repository.branch();

            Path repoPath = cloneOrFetchRepository(repoUrl, branch, repoDir);
            processSingleRepository(repoPath, branch, model);
        }
        LOGGER.info("Completed processing {} repositories", numOfRepositories);
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
    public static Path cloneOrFetchRepository(String repoUrl, @Nullable String branch, String repoDir)
            throws GitAPIException, IOException {
        LOGGER.info("Attempting to clone or fetch repository: {}", repoUrl);

        String repoName = extractRepoName(repoUrl);
        Path repoPath = Paths.get(repoDir, repoName).normalize();
        if (Files.exists(repoPath)) {
            GitUtils.fetchRepository(repoPath);
        } else {
            GitUtils.cloneRepository(repoUrl, branch, repoPath);
        }
        return repoPath;
    }

    /**
     * Processes a given repository by applying the {@link AsheAutomation}
     * process to each Java project root. The method supports processing
     * multiple projects within a single repository.
     *
     * @param repoPath path to the local repository
     * @param branch   the branch of the repository to be processed
     * @throws IOException if an I/O error occurs while accessing the file system
     */
    @SuppressWarnings("nullness:argument")  // Log4J needs to be annotated for nullness
    private static void processSingleRepository(Path repoPath, @Nullable String branch, String model) throws IOException {
        LOGGER.info("Processing repository at: {} for branch: {}", repoPath, branch);

        List<Path> projectRoots = ProjectRootFinder.findJavaRoots(repoPath);
        for (Path projectRoot : projectRoots) {
            LOGGER.info("Processing project root: " + projectRoot.toString());
            AsheAutomation.processAllJavaFiles(projectRoot, projectRoot.toString(), model);
        }

        LOGGER.info("Completed processing repository at: {} for branch: {}", repoPath, branch);
    }

    /**
     * Extracts the repository name from the URL. The repository name is the substring after the last '/'
     * and before an optional '.git' suffix.
     *
     * @param repoUrl URL of the repository
     *                Example: https://github.com/your-username/your-repository.git
     * @return name of the repository extracted from the URL
     * Example: your-repository
     * @throws IllegalArgumentException if the repository URL is invalid or the name cannot be extracted
     */
    private static String extractRepoName(String repoUrl) {
        LOGGER.info("Extracting repository name from URL: {}", repoUrl);

        String origRepoUrl = repoUrl;

        // Remove ".git" suffix
        if (repoUrl.endsWith(".git")) {
            repoUrl = repoUrl.substring(0, repoUrl.length() - ".git".length());
        }

        // Remove the trailing slash
        if (repoUrl.endsWith("/")) {
            repoUrl = repoUrl.substring(0, repoUrl.length() - 1);
        }

        // Extract the repository name after the last slash
        int lastSlashIndex = repoUrl.lastIndexOf('/');
        if (lastSlashIndex != -1) {
            return repoUrl.substring(lastSlashIndex + 1);
        }

        throw new IllegalArgumentException("Invalid repository URL " + origRepoUrl);
    }

    /**
     * The entry point of the application when trying to read and clone repositories from a CSV file.
     *
     * @param args command-line arguments, expected order:
     *             <ol>
     *                 <li>Path to the repositories CSV file.</li>
     *                 <li>Directory for cloning repositories.</li>
     *                 <li>
     *                     LLM argument - "gpt-4" or "manual"
     *                     - gpt-4 will run the GPT-4 model
     *                     - manual will run the manual response the user provides in predefined_responses.txt
     *                 </li>
     *             </ol>
     */
    public static void main(String[] args) {
        if (args.length != 3) {
            LOGGER.error("Provide 3 arguments: the repositories CSV file, the directory in which to clone repositories, and the large language model to use.");
            System.exit(1);
        }

        String csvFilePath = args[0];
        String repoDir = args[1];

        // LLM argument - either gpt-4 or manual (for now)
        String model = args[2];

        // TODO: Add more models here.
        // Example: Arrays.asList("llama", "palm", "grok");
        Set<String> models = new HashSet<>(Arrays.asList("gpt-4", "manual"));
        if (!models.contains(model)) {
            LOGGER.error("Invalid model argument provided: " + model);
            throw new IllegalArgumentException("Invalid model argument provided: " + model);
        }

        readAndProcessRepositoriesCsv(csvFilePath, repoDir, model);
    }
}
