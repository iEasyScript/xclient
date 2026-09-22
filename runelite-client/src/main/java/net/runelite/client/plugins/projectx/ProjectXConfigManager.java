package net.runelite.client.plugins.projectx;

import net.runelite.client.RuneLite;
import net.runelite.client.plugins.Plugin;
import org.slf4j.event.Level;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class ProjectXConfigManager {
    private static final File PROFILES_DIR = new File(RuneLite.RUNELITE_DIR, "projectx-profiles");

    /**
     * Resets all properties in the main properties file that start with the given prefix.
     *
     * @throws IOException
     */
    public static <T extends Plugin> void resetProfilePluginProperties(Class<?> plugin) {
        File profileFile = new File(
                PROFILES_DIR
                        + "/" + ProjectX.getConfigManager().getProfile().getName()
                        + "-"
                        + ProjectX.getConfigManager().getProfile().getId()
                        + ".properties"
        );

        if (!profileFile.isFile()) return;

        try {
            Path tmp = Files.createTempFile(profileFile.toPath().getParent(), profileFile.getName(), ".tmp");

            // Derive the key prefix from class name by stripping the 'Config' suffix
            String name = plugin.getSimpleName().replaceFirst("Plugin$", "").toLowerCase();

            // Match lines like "Name.key=..." or "Name.key:..." (ignores leading spaces)

            try (BufferedReader in = Files.newBufferedReader(profileFile.toPath());
                 BufferedWriter out = Files.newBufferedWriter(tmp)) {

                for (String line; (line = in.readLine()) != null; ) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
                        out.write(line);
                        out.newLine();
                        continue;
                    }
                    if (line.toLowerCase().startsWith(name + ".")) {
                        // Skip this line (i.e., drop it)
                        continue;
                    } else {
                        out.write(line);
                        out.newLine();
                    }
                }
            }

            Files.move(tmp, profileFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            ProjectX.log("Error resetting profile plugin properties: ", Level.ERROR, e.getMessage());
        }
    }
}
