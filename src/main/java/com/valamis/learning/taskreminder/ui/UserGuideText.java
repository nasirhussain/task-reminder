package com.valamis.learning.taskreminder.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Loads the user-guide markdown bundled as a classpath resource. */
final class UserGuideText {

    private static final String RESOURCE_PATH = "/com/valamis/learning/taskreminder/user-guide.md";

    private UserGuideText() {
    }

    static String load() {
        try (InputStream stream = UserGuideText.class.getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                throw new IllegalStateException("Bundled user guide was not found");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append(System.lineSeparator());
                }
                return content.toString();
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load bundled user guide", exception);
        }
    }
}
