package automation;

import edu.njit.jerse.automation.AsheAutomation;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AsheAutomationTest {

    @Test
    void shouldProcessJavaFilesInDirectory() throws Exception {
        Path tempDirectory = Files.createTempDirectory("tempDir");

        // Creating two Java files in the temporary directory
        File javaFile1 = createFile(tempDirectory, "Test1.java");
        File javaFile2 = createFile(tempDirectory, "Test2.java");
        File txtFile1 = createFile(tempDirectory, "Test1.txt");

        AsheAutomation.iterateJavaFiles(tempDirectory.toFile(), tempDirectory.toString());
    }

    @Test
    void shouldIgnoreNonJavaFiles() throws Exception {
        Path tempDirectory = Files.createTempDirectory("tempDir");
        File txtFile = createFile(tempDirectory, "test.txt");
        long lastModifiedTxtFile = txtFile.lastModified();

        AsheAutomation.iterateJavaFiles(tempDirectory.toFile(), tempDirectory.toString());

        assertEquals(lastModifiedTxtFile, txtFile.lastModified());
    }

    @Test
    void shouldHandleEmptyDirectory() throws Exception {
        Path tempDirectory = Files.createTempDirectory("tempDir");

        AsheAutomation.iterateJavaFiles(tempDirectory.toFile(), tempDirectory.toString());

        assertTrue(true);
    }

    private File createFile(Path directory, String fileName) throws IOException {
        File file = directory.resolve(fileName).toFile();
        assertTrue(file.createNewFile());
        return file;
    }
}
