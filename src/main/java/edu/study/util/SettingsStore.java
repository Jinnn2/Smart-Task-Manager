package edu.study.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.study.model.SettingsConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class SettingsStore {
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final Path PATH = Paths.get(System.getProperty("user.home"), ".smart-study", "settings.json");

    private SettingsStore() {
    }

    public static SettingsConfig load() {
        try {
            if (!Files.exists(PATH)) {
                return new SettingsConfig();
            }
            return MAPPER.readValue(PATH.toFile(), SettingsConfig.class);
        } catch (IOException e) {
            return new SettingsConfig();
        }
    }

    public static void save(SettingsConfig cfg) {
        try {
            Files.createDirectories(PATH.getParent());
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(PATH.toFile(), cfg);
        } catch (IOException ignored) {
        }
    }
}
