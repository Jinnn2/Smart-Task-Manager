package edu.study.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class FileUtil {
    private FileUtil() {
    }

    public static Path defaultStoragePath() {
        return Paths.get(System.getProperty("user.home"), ".smart-study", "tasks.json");
    }

    public static void ensureFile(Path path) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to prepare storage file: " + path, e);
        }
    }
}
