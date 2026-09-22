package net.runelite.client.plugins.projectx.util.antiban;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;

public class UnsafeUsageGuardTest {

	private static final Path PROJECTX_SRC = Path.of(
			"src/main/java/net/runelite/client/plugins/projectx");

	@Test
	public void noProjectXSourceImportsSunMiscUnsafe() throws IOException {
		List<String> offenders = new ArrayList<>();
		try (Stream<Path> paths = Files.walk(PROJECTX_SRC)) {
			paths.filter(p -> p.toString().endsWith(".java"))
					.forEach(p -> {
						try {
							for (String line : Files.readAllLines(p)) {
								if (line.contains("sun.misc.Unsafe")) {
									offenders.add(PROJECTX_SRC.relativize(p) + " :: " + line.trim());
									break;
								}
							}
						} catch (IOException ignored) {
						}
					});
		}
		assertTrue("sun.misc.Unsafe must not appear in projectx sources — it's a forensic bot signature. Offenders: " + offenders,
				offenders.isEmpty());
	}
}
