package njit.JerSE.services;

import njit.JerSE.config.Configuration;

import java.io.IOException;
import java.nio.file.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileWatcher {
    Configuration config = new Configuration();
    // Watching the file ASHEExamples is outputting to for any errors or warnings
    private final String FILE_PATH = config.getPropertyValue("watched.file.path");

    public String watchForErrors() {
        Path filePath = Paths.get(FILE_PATH);
        Path path = filePath.getParent();

        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {

            path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

            while (true) {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    // TODO: check for other events, such as creation, deletion, etc.
                    // as of now, we are only checking for modifications to the directory
                    if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                        String changed = event.context().toString();
                        // when the file we are watching is modified, we check for errors
                        if (changed.equals(filePath.getFileName().toString())) {
                            String errorMessage = extractErrors();
                            if (!errorMessage.isEmpty()) {
                                return errorMessage;
                            }
                        }
                    }
                }
                key.reset();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private String extractErrors() throws IOException {
        // Read the entire file into a string
        String errorsFile = new String(Files.readAllBytes(Paths.get(FILE_PATH)));

        // TODO: Potentially find a better way to extract errors
        // TODO: An option is to figure out how to print the errors/warnings ONLY instead of the entire output
        // get everything between "error:" and "FAILURE:" in the txt file
        String errorsRegex = "(error:.*?(?=FAILURE:))";

        // Compile the provided regex pattern considering the DOTALL flag.
        // The DOTALL flag allows the dot (.) in the regex to match line terminators (e.g., '\n').
        Pattern pattern = Pattern.compile(errorsRegex, Pattern.DOTALL);

        // Match the compiled pattern against the content of errorsFile.
        Matcher matcher = pattern.matcher(errorsFile);
        if (matcher.find()) {
            // TODO: Prove this is the proper group to return
            // matcher.group(1) returns the first capturing group: "error: Some error description here."
            return matcher.group(1).trim();
        }
        return "";
    }
}
