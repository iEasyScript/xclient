package net.runelite.client.plugins.projectx.externalplugins;

import com.google.gson.Gson;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.projectx.ProjectXConfig;
import net.runelite.client.ui.ClientUI;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProjectXInstancesTest
{
    private static MockWebServer site;

    private ConfigManager configManager;
    private ScheduledExecutorService executor;
    private ScheduledFuture<?> beating;
    private ClientUI clientUi;
    private ProjectXInstances instances;

    @BeforeClass
    public static void startSite() throws IOException
    {
        site = new MockWebServer();
        site.start();
    }

    @AfterClass
    public static void stopSite() throws IOException
    {
        site.shutdown();
    }

    @Before
    public void setUp() throws InterruptedException
    {
        // The site is shared by the whole class, and a test that enqueues a
        // response without reading the request leaves it queued. Drain it, or an
        // assertion about this test's request reads the previous test's.
        while (site.takeRequest(1, TimeUnit.MILLISECONDS) != null)
        {
            // discarded
        }

        // A non-loopback host, so the offline allowance for local development
        // does not mask what these tests are checking.
        System.setProperty("projectx.siteUrl", "https://xclient.dev");

        configManager = mock(ConfigManager.class);
        when(configManager.getConfiguration(ProjectXConfig.configGroup, ProjectXConfig.keyAccountToken))
                .thenReturn("px_test");

        executor = mock(ScheduledExecutorService.class);
        beating = mock(ScheduledFuture.class);
        doReturn(beating).when(executor)
                .scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));

        clientUi = mock(ClientUI.class);

        instances = new ProjectXInstances(new OkHttpClient(), new Gson(), configManager, executor,
                () -> clientUi);
    }

    @After
    public void clearOverride()
    {
        System.clearProperty("projectx.siteUrl");
    }

    /** Points the client at the mock site for tests that need a real response. */
    private void useMockSite()
    {
        System.setProperty("projectx.siteUrl", site.url("/").toString());
    }

    /**
     * The heartbeat the claim scheduled, so a test can run one on demand.
     *
     * The executor is a mock, so nothing is running in the background and the
     * beat happens exactly when it is asked for.
     */
    private Runnable heartbeat()
    {
        ArgumentCaptor<Runnable> beat = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).scheduleWithFixedDelay(beat.capture(), anyLong(), anyLong(), any(TimeUnit.class));
        return beat.getValue();
    }

    @Test
    public void claimsASlotWhenOneIsFree() throws InterruptedException
    {
        useMockSite();
        site.enqueue(new MockResponse().setBody("{\"ok\":true,\"allowance\":{\"total\":2,\"used\":1}}"));

        assertTrue(instances.claim());

        RecordedRequest request = site.takeRequest();
        assertEquals("/api/v1/instances", request.getPath());
        assertEquals("Bearer px_test", request.getHeader("Authorization"));
        assertTrue(request.getBody().readUtf8().contains("\"action\":\"claim\""));
    }

    @Test
    public void refusesWhenNoSlotIsFree()
    {
        useMockSite();
        site.enqueue(new MockResponse().setResponseCode(409)
                .setBody("{\"error\":\"no_instance_available\",\"message\":\"You are using 1 of 1 instances.\"}"));

        assertFalse(instances.claim());
        assertTrue(instances.getRefusal().contains("You are using 1 of 1 instances."));
        // The message must say how to fix it, not just that it failed.
        assertTrue(instances.getRefusal().contains("instances"));
    }

    @Test
    public void refusesWithoutAToken()
    {
        when(configManager.getConfiguration(ProjectXConfig.configGroup, ProjectXConfig.keyAccountToken))
                .thenReturn("");

        assertFalse(instances.claim());
        assertTrue(instances.getRefusal().contains("API token"));
    }

    @Test
    public void refusesWhenTheTokenIsRejected()
    {
        useMockSite();
        site.enqueue(new MockResponse().setResponseCode(401));

        assertFalse(instances.claim());
        assertTrue(instances.getRefusal().contains("not accepted"));
    }

    /** An unreachable site must not become a way to run unlimited clients. */
    @Test
    public void refusesWhenTheSiteCannotBeReached()
    {
        System.setProperty("projectx.siteUrl", "https://127.0.0.2:9");

        assertFalse(instances.claim());
        assertTrue(instances.getRefusal().contains("Could not reach"));
    }

    /**
     * Stop on the website has to reach the client, or the button is a lie: it
     * would free the slot on the site while the client carried on running.
     */
    @Test
    public void shutsDownWhenStoppedFromTheWebsite() throws InterruptedException
    {
        useMockSite();
        site.enqueue(new MockResponse().setBody("{\"ok\":true}"));
        site.enqueue(new MockResponse().setResponseCode(409)
                .setBody("{\"error\":\"stopped\",\"message\":\"This client was stopped from the website.\"}"));

        assertTrue(instances.claim());
        site.takeRequest();

        heartbeat().run();

        assertTrue(site.takeRequest().getBody().readUtf8().contains("\"action\":\"heartbeat\""));
        verify(clientUi).requestShutdown();
        // Otherwise a beat during the shutdown claims a fresh slot, and the
        // client reappears on the very list it was just taken off.
        verify(beating).cancel(false);
    }

    /**
     * A lapsed rental is not a stop. Killing a client mid-script over it would
     * lose the user whatever the script was partway through.
     */
    @Test
    public void keepsRunningWhenTheSlotLapses()
    {
        useMockSite();
        site.enqueue(new MockResponse().setBody("{\"ok\":true}"));
        site.enqueue(new MockResponse().setResponseCode(409)
                .setBody("{\"error\":\"no_instance_available\",\"message\":\"You are using 2 of 1 instances.\"}"));

        assertTrue(instances.claim());
        heartbeat().run();

        verify(clientUi, never()).requestShutdown();
    }

    /** Except in local development, where the site is this machine. */
    @Test
    public void allowsStartWhenTheLocalSiteIsNotRunning()
    {
        System.setProperty("projectx.siteUrl", "http://localhost:9");

        assertTrue(instances.claim());
    }
}
