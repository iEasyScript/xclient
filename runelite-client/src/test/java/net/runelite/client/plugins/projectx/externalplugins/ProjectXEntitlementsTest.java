package net.runelite.client.plugins.projectx.externalplugins;

import com.google.gson.Gson;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.projectx.ProjectXConfig;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProjectXEntitlementsTest
{
    private static MockWebServer site;

    private ConfigManager configManager;
    private PluginManager pluginManager;
    private ProjectXEntitlements entitlements;

    @PluginDescriptor(name = "Paid", isExternal = true)
    public static class PaidPlugin extends Plugin
    {
    }

    @PluginDescriptor(name = "Free", isExternal = true)
    public static class FreePlugin extends Plugin
    {
    }

    @PluginDescriptor(name = "Core")
    public static class CorePlugin extends Plugin
    {
    }

    @BeforeClass
    public static void startSite() throws IOException
    {
        site = new MockWebServer();
        site.start();
        // ProjectXSite reads this once, when first used.
        System.setProperty("projectx.siteUrl", site.url("/").toString());
    }

    @AfterClass
    public static void stopSite() throws IOException
    {
        site.shutdown();
    }

    @Before
    public void setUp()
    {
        configManager = mock(ConfigManager.class);
        pluginManager = mock(PluginManager.class);
        when(configManager.getConfiguration(ProjectXConfig.configGroup, ProjectXConfig.keyAccountToken))
                .thenReturn("px_test");

        entitlements = new ProjectXEntitlements(new OkHttpClient(), new Gson(), configManager, pluginManager,
                mock(ScheduledExecutorService.class), mock(Notifier.class));

        ProjectXPluginManifest paid = new ProjectXPluginManifest();
        paid.setInternalName("PaidPlugin");
        paid.setPaid(true);
        ProjectXPluginManifest free = new ProjectXPluginManifest();
        free.setInternalName("FreePlugin");
        entitlements.updatePaidPlugins(List.of(paid, free));
    }

    private static MockResponse result(boolean active, Instant expiresAt)
    {
        String expiry = expiresAt == null ? "null" : "\"" + expiresAt + "\"";
        return new MockResponse().setBody("{\"results\":[{\"internalName\":\"PaidPlugin\",\"active\":" + active
                + ",\"expiresAt\":" + expiry + "}]}");
    }

    @Test
    public void activeSubscriptionIsEntitledAndRequestCarriesToken() throws InterruptedException
    {
        site.enqueue(result(true, Instant.now().plus(7, ChronoUnit.DAYS)));

        entitlements.refresh();

        assertTrue(entitlements.isEntitled("PaidPlugin"));
        RecordedRequest request = site.takeRequest();
        assertEquals("/api/v1/entitlements", request.getPath());
        assertEquals("Bearer px_test", request.getHeader("Authorization"));
        assertEquals("{\"internalNames\":[\"PaidPlugin\"]}", request.getBody().readUtf8());
    }

    @Test
    public void inactiveSubscriptionIsNotEntitled()
    {
        site.enqueue(result(false, null));

        entitlements.refresh();

        assertFalse(entitlements.isEntitled("PaidPlugin"));
    }

    @Test
    public void accessEndsAtExpiryWithoutAskingTheSiteAgain()
    {
        site.enqueue(result(true, Instant.now().minusSeconds(1)));

        entitlements.refresh();

        assertFalse(entitlements.isEntitled("PaidPlugin"));
    }

    @Test
    public void outageKeepsCachedAccess()
    {
        site.enqueue(result(true, Instant.now().plus(7, ChronoUnit.DAYS)));
        entitlements.refresh();

        site.enqueue(new MockResponse().setResponseCode(500));
        entitlements.refresh();

        assertTrue(entitlements.isEntitled("PaidPlugin"));
    }

    @Test
    public void rejectedTokenRevokesAccess()
    {
        site.enqueue(result(true, Instant.now().plus(7, ChronoUnit.DAYS)));
        entitlements.refresh();

        site.enqueue(new MockResponse().setResponseCode(401));
        entitlements.refresh();

        assertFalse(entitlements.isEntitled("PaidPlugin"));
    }

    @Test
    public void gateBlocksPaidPluginWithoutAccess()
    {
        Plugin plugin = new PaidPlugin();

        assertFalse(entitlements.mayStart(plugin));
        verify(pluginManager).setPluginEnabled(plugin, false);
    }

    @Test
    public void gateAllowsPaidPluginWithAccess()
    {
        site.enqueue(result(true, Instant.now().plus(7, ChronoUnit.DAYS)));
        entitlements.refresh();

        assertTrue(entitlements.mayStart(new PaidPlugin()));
    }

    @Test
    public void gateNeverTouchesFreeOrCorePlugins()
    {
        assertTrue(entitlements.mayStart(new FreePlugin()));
        assertTrue(entitlements.mayStart(new CorePlugin()));
        verify(pluginManager, never()).setPluginEnabled(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    public void paidListSurvivesAManifestThatOmitsIt()
    {
        entitlements.updatePaidPlugins(List.of());

        assertTrue(entitlements.isPaid("PaidPlugin"));
    }
}
