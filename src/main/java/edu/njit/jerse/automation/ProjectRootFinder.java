package edu.njit.jerse.automation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * The {@code ProjectRootFinder} class is responsible for scanning a given directory
 * and its subdirectories to identify Java project roots. It primarily looks for directories
 * that conform to standard Java source directory structures, excluding test directories.
 */
public class ProjectRootFinder {
    private static final Logger LOGGER = LogManager.getLogger(ProjectRootFinder.class);

    /**
     * Finds Java project roots in the given directory and its subdirectories.
     *
     * @param directory the root directory from which the search begins
     * @return a list of {@code File} objects, each representing a discovered Java project root
     */
    public static List<File> findJavaRoots(File directory) throws IOException {
        LOGGER.debug("Searching for Java roots in directory: {}", directory.getAbsolutePath());

        List<File> javaRoots = new ArrayList<>();

        Files.walkFileTree(directory.toPath(), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                File dirFile = dir.toFile();

                if (isTestDirectory(dirFile)) {
                    LOGGER.info("Skipping test directory: {}", dirFile.getAbsolutePath());
                    // Skip test directories
                    return FileVisitResult.SKIP_SUBTREE;
                }

                if (isJavaSourceRoot(dirFile)) {
                    LOGGER.info("Found Java root: {}", dirFile.getAbsolutePath());
                    javaRoots.add(dirFile);
                    // Once a Java source root is found, skip its subdirectories
                    return FileVisitResult.SKIP_SUBTREE;
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException e) {
                LOGGER.error("Failed to visit file: " + file, e);
                return FileVisitResult.CONTINUE;
            }
        });

        LOGGER.info("Java root search completed in directory: {}", directory.getAbsolutePath());
        return javaRoots;
    }

    /**
     * Determines if the given directory is a Java source root.
     * A directory is considered a Java source root if it ends with '/src/main/java'.
     *
     * @param directory the directory to check
     * @return {@code true} if the directory is a Java source root; {@code false} otherwise
     */
    private static boolean isJavaSourceRoot(File directory) {
        String path = directory.getAbsolutePath().replace("\\", "/");
        return path.endsWith("/src/main/java");
    }

    /**
     * Determines if the given directory is a test directory.
     * Test directories are identified by their paths containing '/test/' or '/tests/'.
     *
     * @param directory the directory to check
     * @return {@code true} if the directory is a test directory; {@code false} otherwise
     */
    private static boolean isTestDirectory(File directory) {
        String path = directory.getAbsolutePath().replace("\\", "/");
        return path.contains("/test/") || path.contains("/tests/");
    }
}
