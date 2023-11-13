package automation;

import edu.njit.jerse.automation.ProjectRootScanner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ProjectRootScannerTest {
    @SuppressWarnings("initialization.field.uninitialized")
    @TempDir
    Path tempDir;

    @SuppressWarnings("initialization.field.uninitialized")
    File root;

    @BeforeEach
    void setUp() {
        root = tempDir.toFile();
    }

    @AfterEach
    void tearDown() {
        // Cleanup is handled by the @TempDir feature
    }

    @Test
    void testFindJavaRoots_WithJavaRoots() {
        File mainJava = tempDir.resolve("src/main/java").toFile();
        assertTrue(mainJava.mkdirs(), "Could not create Java source directory structure.");

        List<File> javaRoots = ProjectRootScanner.findJavaRoots(root);

        assertEquals(1, javaRoots.size(), "Expected to find one Java root.");
        assertEquals(mainJava.getAbsolutePath(), javaRoots.get(0).getAbsolutePath(), "The Java root paths should match.");
    }

    @Test
    void testFindJavaRoots_WithTestDirectories() throws IOException {
        File testJava = tempDir.resolve("src/test/java").toFile();
        assertTrue(testJava.mkdirs(), "Could not create test directory structure.");

        List<File> javaRoots = ProjectRootScanner.findJavaRoots(root);

        assertTrue(javaRoots.isEmpty(), "Test directories should not be considered as Java roots.");
    }

    @Test
    void testFindJavaRoots_NoJavaRoots() {
        List<File> javaRoots = ProjectRootScanner.findJavaRoots(root);

        assertTrue(javaRoots.isEmpty(), "Should not find Java roots in an empty directory.");
    }
}
