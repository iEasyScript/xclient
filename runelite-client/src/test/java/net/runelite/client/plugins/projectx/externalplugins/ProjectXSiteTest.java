package net.runelite.client.plugins.projectx.externalplugins;

import okhttp3.HttpUrl;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProjectXSiteTest
{
    @After
    public void clearOverride()
    {
        System.clearProperty("projectx.siteUrl");
    }

    @Test
    public void followsTheConfiguredSite()
    {
        System.setProperty("projectx.siteUrl", "https://xclient.dev");

        assertEquals("https://xclient.dev/api/v1/hub/plugins.json",
                ProjectXSite.api("hub", "plugins.json").toString());
        assertTrue(ProjectXSite.isSite(HttpUrl.get("https://xclient.dev/api/v1/hub/jars/X/1.0")));
        assertFalse(ProjectXSite.isSite(HttpUrl.get("https://example.com/api/v1/hub")));
    }

    @Test
    public void changingTheSettingTakesEffectWithoutRestart()
    {
        System.setProperty("projectx.siteUrl", "https://xclient.dev");
        assertEquals("xclient.dev", ProjectXSite.baseUrl().host());

        System.setProperty("projectx.siteUrl", "http://localhost:3000");
        assertEquals("localhost", ProjectXSite.baseUrl().host());
    }

    @Test
    public void unusableUrlFallsBackInsteadOfThrowing()
    {
        System.setProperty("projectx.siteUrl", "not a url");

        assertEquals("localhost", ProjectXSite.baseUrl().host());
    }

    @Test
    public void jarsOnlyComeOverHttpsOrFromThisMachine()
    {
        assertTrue(ProjectXSite.isAllowedDownload(HttpUrl.get("https://xclient.dev/a.jar")));
        assertTrue(ProjectXSite.isAllowedDownload(HttpUrl.get("http://localhost:3000/a.jar")));
        assertFalse(ProjectXSite.isAllowedDownload(HttpUrl.get("http://example.com/a.jar")));
    }
}
