package edu.njit.jerse.automation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The {@code ProjectRootScanner} class is responsible for scanning a given directory
 * and its subdirectories to identify Java project roots. It primarily looks for directories
 * that conform to standard Java source directory structures, excluding test directories.
 */
public class ProjectRootScanner {
    private static final Logger LOGGER = LogManager.getLogger(ProjectRootScanner.class);

    /**
     * Scans the given directory and its subdirectories to find Java project roots.
     * This method initializes the search and adds found Java roots to a list.
     *
     * @param directory The root directory from which the search begins.
     * @return A list of {@code File} objects, each representing a discovered Java project root.
     */
    public static List<File> findJavaRoots(File directory) {
        List<File> javaRoots = new ArrayList<>();
        recursiveSearchForJavaRoots(directory, javaRoots);
        return javaRoots;
    }

    /**
     * Recursively searches for Java project roots within a directory and its subdirectories.
     * It excludes directories identified as test directories and adds Java source directories
     * to the provided list.
     *
     * @param directory The directory to search for Java project roots.
     * @param javaRoots The list where discovered Java project roots are accumulated.
     */
    private static void recursiveSearchForJavaRoots(File directory, List<File> javaRoots) {
        LOGGER.debug("Searching for Java roots in directory: {}", directory.getAbsolutePath());

        if (directory.isDirectory()) {
            if (isTestDirectory(directory)) {
                LOGGER.debug("Skipping test directory: {}", directory.getAbsolutePath());
                return;
            }

            if (isJavaSourceRoot(directory)) {
                javaRoots.add(directory);
                return;
            }


            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        recursiveSearchForJavaRoots(file, javaRoots);
                    }
                }
            }
        }
        LOGGER.debug("Java root search completed in directory: {}", directory.getAbsolutePath());
    }

    /**
     * Determines if the given directory is a Java source root.
     * A directory is considered a Java source root if it follows a standard Java
     * source directory structure, typically ending with '/src/main/java'.
     *
     * @param directory The directory to check.
     * @return {@code true} if the directory is a Java source root; {@code false} otherwise.
     */
    private static boolean isJavaSourceRoot(File directory) {
        String path = directory.getAbsolutePath().replace("\\", "/");
        return path.endsWith("/src/main/java");
    }

    /**
     * Determines if the given directory is a test directory.
     * Test directories are identified by their paths containing '/test/' or '/tests/'.
     *
     * @param directory The directory to check.
     * @return {@code true} if the directory is a test directory; {@code false} otherwise.
     */
    private static boolean isTestDirectory(File directory) {
        String path = directory.getAbsolutePath().replace("\\", "/");
        return path.contains("/test/") || path.contains("/tests/");
    }
}
