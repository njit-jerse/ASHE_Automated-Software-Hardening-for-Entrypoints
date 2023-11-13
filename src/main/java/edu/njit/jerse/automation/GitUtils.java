package edu.njit.jerse.automation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;

/**
 * The {@code GitUtils} class encapsulates operations for interacting with Git repositories
 * using the JGit library. It serves as a utility class to abstract common Git operations
 * like cloning a repository or fetching updates from a remote repository.
 * <p>
 * This class provides a streamlined interface for initiating Git processes programmatically,
 * allowing for the cloning of specific branches and updating local copies of repositories
 * with changes from their remote sources. It is designed to integrate with applications
 * that require direct manipulation of Git repositories without direct command line interaction.
 */
public class GitUtils {
    private static final Logger LOGGER = LogManager.getLogger(GitUtils.class);

    /**
     * Clones a Git repository from a specified remote URL into a local directory.
     * The method allows specifying a particular branch to clone.
     *
     * @param repoUrl   The URL of the remote repository to clone.
     * @param branch    The branch of the repository to clone. If null, the default branch is cloned.
     * @param directory The directory where the repository will be cloned into. It should not exist or
     *                  be an empty directory.
     * @throws GitAPIException If any error occurs during the cloning process.
     */
    public static void cloneRepository(String repoUrl, String branch, File directory) throws GitAPIException {
        LOGGER.info("Cloning repository: " + repoUrl);
        Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(directory)
                .setBranch(branch)
                .call();
    }

    /**
     * Fetches updates from the remote repository associated with a local repository path.
     * This method is used to update the local repository with changes from its remote counterpart without
     * merging those changes.
     *
     * @param repoPath The path to the local repository which is set up to track a remote repository.
     * @throws IOException     If the local repository path cannot be accessed.
     * @throws GitAPIException If any error occurs during the fetch operation.
     */
    public static void fetchRepository(File repoPath) throws IOException, GitAPIException {
        LOGGER.info("Fetching changes for repository: " + repoPath);
        try (Git git = Git.open(repoPath)) {
            git.fetch().call();
        }
    }
}
