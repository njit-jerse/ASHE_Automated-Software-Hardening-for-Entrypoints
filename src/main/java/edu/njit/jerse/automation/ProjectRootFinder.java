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
 * The {@link ProjectRootFinder} class scans a directory and its subdirectories to find Java project roots.
 * It identifies project roots by looking for directories that follow the standard Java source
 * directory structure, src/main/java, and excludes test-related directories.
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
                String dirFilePath = dirFile.getAbsolutePath().replace("\\", "/");

                boolean isTestDirectory = dirFilePath.contains("/test/") || dirFilePath.contains("/tests/");
                if (isTestDirectory) {
                    LOGGER.info("Skipping test directory: {}", dirFilePath);
                    // Skip test directories
                    return FileVisitResult.SKIP_SUBTREE;
                }

                boolean isJavaRoot = dirFilePath.endsWith(AsheAutomation.JAVA_SOURCE_DIR);
                if (isJavaRoot) {
                    LOGGER.info("Found Java root: {}", dirFilePath);
                    javaRoots.add(dirFile);
                    // Once a Java source root is found, skip its subdirectories
                    return FileVisitResult.SKIP_SUBTREE;
                }

                return FileVisitResult.CONTINUE;
            }
        });

        LOGGER.info("Java root search completed in directory: {}", directory.getAbsolutePath());
        return javaRoots;
    }
}
