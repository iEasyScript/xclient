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

    // ------------------------------------------------------------------ free trials

    /** A fake monotonic clock, in nanoseconds, that the tests move by hand. */
    private final long[] nanos = {1_000_000_000L};

    private void useFakeClock()
    {
        entitlements.nanoClock = () -> nanos[0];
    }

    private void advance(long seconds)
    {
        nanos[0] += seconds * 1_000_000_000L;
    }

    /** A trial answer as the site sends it: its own clock in checkedAt, and the trial flag. */
    private static MockResponse trial(Instant checkedAt, Instant endsAt)
    {
        return new MockResponse().setBody("{\"checkedAt\":\"" + checkedAt + "\",\"results\":[{\"internalName\":\"PaidPlugin\","
                + "\"active\":true,\"expiresAt\":\"" + endsAt + "\",\"trial\":true}]}");
    }

    @Test
    public void trialIsEntitledUntilItsHourIsUp()
    {
        useFakeClock();
        Instant now = Instant.now();
        site.enqueue(trial(now, now.plus(60, ChronoUnit.MINUTES)));
        entitlements.refresh();
        assertTrue(entitlements.isEntitled("PaidPlugin"));

        // Still confirmed by the site each minute, as the client does on a trial.
        for (int minute = 1; minute < 60; minute++)
        {
            advance(60);
            site.enqueue(trial(now.plus(minute, ChronoUnit.MINUTES), now.plus(60, ChronoUnit.MINUTES)));
            entitlements.refresh();
        }
        assertTrue(entitlements.isEntitled("PaidPlugin"));

        advance(61);
        assertFalse(entitlements.isEntitled("PaidPlugin"));
    }

    @Test
    public void trialStopsWhenTheSiteCannotBeReached()
    {
        useFakeClock();
        Instant now = Instant.now();
        site.enqueue(trial(now, now.plus(60, ChronoUnit.MINUTES)));
        entitlements.refresh();

        // Blocked from the site: the cached hour is not enough on its own.
        site.enqueue(new MockResponse().setResponseCode(500));
        advance(60);
        entitlements.refresh();
        assertTrue("one missed check is tolerated", entitlements.isEntitled("PaidPlugin"));

        site.enqueue(new MockResponse().setResponseCode(500));
        advance(60);
        entitlements.refresh();
        advance(31);
        assertFalse("no answer for over 2.5 minutes ends the trial", entitlements.isEntitled("PaidPlugin"));
    }

    @Test
    public void paidAccessStillRidesOutAnOutage()
    {
        useFakeClock();
        site.enqueue(result(true, Instant.now().plus(7, ChronoUnit.DAYS)));
        entitlements.refresh();

        site.enqueue(new MockResponse().setResponseCode(500));
        advance(600);
        entitlements.refresh();

        assertTrue(entitlements.isEntitled("PaidPlugin"));
    }

    @Test
    public void settingThePcClockBackDoesNotStretchATrial()
    {
        useFakeClock();
        // The PC clock is a day behind the site: by the wall clock the trial looks like
        // it has a day and a minute left. The site says one minute, and that is what counts.
        Instant siteNow = Instant.now().plus(1, ChronoUnit.DAYS);
        site.enqueue(trial(siteNow, siteNow.plus(1, ChronoUnit.MINUTES)));
        entitlements.refresh();
        assertTrue(entitlements.isEntitled("PaidPlugin"));

        advance(61);
        assertFalse(entitlements.isEntitled("PaidPlugin"));
    }

    @Test
    public void endedTrialIsNotEntitledOnceTheSiteSaysSo()
    {
        useFakeClock();
        Instant now = Instant.now();
        site.enqueue(trial(now, now.plus(60, ChronoUnit.MINUTES)));
        entitlements.refresh();

        site.enqueue(result(false, null));
        advance(60);
        entitlements.refresh();

        assertFalse(entitlements.isEntitled("PaidPlugin"));
        assertFalse(entitlements.mayStart(new PaidPlugin()));
    }

    @Test
    public void paidListSurvivesAManifestThatOmitsIt()
    {
        entitlements.updatePaidPlugins(List.of());

        assertTrue(entitlements.isPaid("PaidPlugin"));
    }
}
