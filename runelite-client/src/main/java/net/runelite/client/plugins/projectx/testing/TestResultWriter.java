package net.runelite.client.plugins.projectx.testing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class TestResultWriter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void write(TestResult result) {
        write(result, getDefaultOutputDir());
    }

    public static void write(TestResult result, String outputDir) {
        try {
            Path dir = Paths.get(outputDir);
            Files.createDirectories(dir);

            attachLogTail(result);

            File resultFile = dir.resolve("result.json").toFile();
            try (FileWriter writer = new FileWriter(resultFile)) {
                GSON.toJson(result, writer);
            }

            log.info("Test result written to {}", resultFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to write test result", e);
        }
    }

    private static void attachLogTail(TestResult result) {
        try {
            String logPath = System.getProperty("user.home") + "/.runelite/logs/client.log";
            File logFile = new File(logPath);
            if (!logFile.exists()) return;

            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }

            int start = Math.max(0, lines.size() - 200);
            result.logTail = String.join("\n", lines.subList(start, lines.size()));
        } catch (IOException e) {
            log.warn("Could not read log file for test result", e);
        }
    }

    public static String getDefaultOutputDir() {
        String custom = System.getProperty("projectx.test.output");
        if (custom != null && !custom.isEmpty()) {
            return custom;
        }
        return System.getProperty("user.home") + "/.runelite/test-results";
    }
}
