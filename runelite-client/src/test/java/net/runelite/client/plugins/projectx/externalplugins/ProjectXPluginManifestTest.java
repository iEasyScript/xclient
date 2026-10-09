package net.runelite.client.plugins.projectx.externalplugins;

import com.google.gson.Gson;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * A script the store renames keeps its old class names in the catalogue, which is
 * how the client retires the old build and carries its on/off state across.
 */
public class ProjectXPluginManifestTest
{
    private final Gson gson = new Gson();

    @Test
    public void readsTheNamesARenamedScriptHadBefore()
    {
        ProjectXPluginManifest m = gson.fromJson(
            "{\"internalName\":\"MadCowPlugin\",\"version\":\"0.1.62\",\"previousInternalNames\":[\"OldMadCowPlugin\"]}",
            ProjectXPluginManifest.class);
        assertEquals("MadCowPlugin", m.getInternalName());
        assertEquals(List.of("OldMadCowPlugin"), m.getPreviousInternalNames());
    }

    @Test
    public void aScriptThatWasNeverRenamedHasNone()
    {
        ProjectXPluginManifest m = gson.fromJson("{\"internalName\":\"AgilityCoursesPlugin\",\"version\":\"1.3.6\"}",
            ProjectXPluginManifest.class);
        assertNull(m.getPreviousInternalNames());
    }
}
