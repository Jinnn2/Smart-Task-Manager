package edu.study.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Minimal sandbox runner to execute jar files under a Tools directory.
 * It does not provide OS-level isolation but keeps execution scoped to the directory and captures output.
 */
public class ToolSandboxRunner {
    private final Path toolsDir;

    public ToolSandboxRunner(Path toolsDir) {
        this.toolsDir = toolsDir;
    }

    public List<String> listJars() {
        try {
            if (!Files.isDirectory(toolsDir)) {
                return Collections.emptyList();
            }
            try (var stream = Files.list(toolsDir)) {
                return stream
                        .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".jar"))
                        .map(p -> p.getFileName().toString())
                        .sorted()
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    public void runJar(String jarName, java.util.function.Consumer<String> logger) {
        Path jarPath = toolsDir.resolve(jarName);
        if (!Files.exists(jarPath)) {
            logger.accept("[error] jar not found: " + jarPath);
            return;
        }
        ProcessBuilder pb = new ProcessBuilder("java", "-Dfile.encoding=UTF-8", "-jar", jarPath.toString());
        pb.directory(toolsDir.toFile());
        pb.redirectErrorStream(true);
        new Thread(() -> {
            try {
                logger.accept("[start] " + jarName);
                Process p = pb.start();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logger.accept(line);
                    }
                }
                int exit = p.waitFor();
                logger.accept("[exit] " + jarName + " code=" + exit);
            } catch (Exception e) {
                logger.accept("[error] " + e.getMessage());
            }
        }, "tool-jar-runner-" + jarName).start();
    }
}
