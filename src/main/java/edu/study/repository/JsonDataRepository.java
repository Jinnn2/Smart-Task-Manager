package edu.study.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonDataRepository {
    private final Path storagePath;
    private final ObjectMapper mapper;

    public JsonDataRepository(Path storagePath) {
        this.storagePath = storagePath;
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    public synchronized DataStore load() {
        if (!Files.exists(storagePath)) {
            return new DataStore();
        }
        try {
            if (Files.size(storagePath) == 0) {
                return new DataStore();
            }
            return mapper.readValue(storagePath.toFile(), DataStore.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read storage file: " + storagePath, e);
        }
    }

    public synchronized void save(DataStore store) {
        try {
            if (storagePath.getParent() != null) {
                Files.createDirectories(storagePath.getParent());
            }
            mapper.writeValue(storagePath.toFile(), store);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write storage file: " + storagePath, e);
        }
    }
}
