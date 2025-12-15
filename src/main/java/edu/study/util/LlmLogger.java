package edu.study.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class LlmLogger {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HHmmssSSS");

    private LlmLogger() {
    }

    public static void log(String type, String prompt, String response) {
        try {
            Path dir = Paths.get("logs", LocalDate.now().toString());
            Files.createDirectories(dir);
            String filename = type + "-" + TS.format(LocalDateTime.now()) + ".txt";
            Path file = dir.resolve(filename);
            String content = "Prompt:\n" + (prompt == null ? "" : prompt) + "\n\nResponse:\n" + (response == null ? "" : response);
            Files.write(file, content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {
            // logging failure should not break app
        }
    }
}
