package net.runelite.client.plugins.projectx.externalplugins;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import com.google.gson.Gson;
import java.util.List;
import org.junit.Test;

public class ProjectXDevPluginGuardTest
{
	private final List<ProjectXPluginManifest> store = List.of(new Gson().fromJson(
		"{\"internalName\":\"AgilityCoursesPlugin\",\"sha256\":\"abc123\",\"artifactId\":\"agilitycourses\"}",
		ProjectXPluginManifest.class));

	@Test
	public void refusesAStoreName()
	{
		assertNotNull(ProjectXDevPluginGuard.refusal("agilitycoursesplugin", "my.pkg", "ff", store));
	}

	@Test
	public void refusesAStoreHash()
	{
		assertNotNull(ProjectXDevPluginGuard.refusal("MyPlugin", "my.pkg", "ABC123", store));
	}

	@Test
	public void refusesAStorePackage()
	{
		assertNotNull(ProjectXDevPluginGuard.refusal("MyPlugin",
			"net.runelite.client.plugins.projectx.agilitycourses.sub", "ff", store));
	}

	@Test
	public void allowsAnUnknownPlugin()
	{
		assertNull(ProjectXDevPluginGuard.refusal("LumbridgeFishCookPlugin", "dev.lumbridge", "ff", store));
	}
}
