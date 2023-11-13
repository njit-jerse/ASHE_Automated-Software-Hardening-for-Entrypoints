package automation;

import edu.njit.jerse.automation.GitUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class GitUtilsTest {

    @SuppressWarnings("initialization.field.uninitialized")
    @TempDir
    Path tempDir;

    @SuppressWarnings("initialization.field.uninitialized")
    File repoDirectory;
    String repoUrl = "https://github.com/jonathan-m-phillips/ASHE_Automated-Software-Hardening-for-Entrypoints"; // Use a real or dummy repository URL
    String branch = "main";

    @BeforeEach
    void setUp() {
        repoDirectory = tempDir.toFile();
    }

    @AfterEach
    void tearDown() {
        // Cleanup is handled by the @TempDir feature
    }

    @Test
    void testCloneRepository() {
        assertDoesNotThrow(() -> GitUtils.cloneRepository(repoUrl, branch, repoDirectory));
    }

    @Test
    void testFetchRepository() throws GitAPIException {
        GitUtils.cloneRepository(repoUrl, branch, repoDirectory);
        assertDoesNotThrow(() -> GitUtils.fetchRepository(repoDirectory));
    }
}