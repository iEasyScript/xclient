package net.runelite.client.plugins.projectx.externalplugins;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.projectx.ProjectXConfig;
import net.runelite.client.ui.ClientUI;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Website control riding on the instance heartbeat, without breaking what the heartbeat already does. */
public class ProjectXInstancesExtensionTest
{
    private MockWebServer site;
    private ScheduledExecutorService executor;
    private ClientUI clientUi;
    private ProjectXInstances instances;
    private final List<JsonObject> answers = new ArrayList<>();
    private boolean live = true;

    private final ProjectXInstances.Extension extension = new ProjectXInstances.Extension()
    {
        @Override
        public void addTo(JsonObject body)
        {
            JsonObject remote = new JsonObject();
            remote.addProperty("v", 1);
            body.add("remote", remote);
        }

        @Override
        public void answered(JsonObject answer)
        {
            answers.add(answer);
        }

        @Override
        public boolean wantsLiveHeartbeat()
        {
            return live;
        }
    };

    @Before
    public void setUp() throws IOException
    {
        site = new MockWebServer();
        site.start();
        System.setProperty("projectx.siteUrl", site.url("/").toString());

        ConfigManager configManager = mock(ConfigManager.class);
        when(configManager.getConfiguration(ProjectXConfig.configGroup, ProjectXConfig.keyAccountToken)).thenReturn("px_test");
        executor = mock(ScheduledExecutorService.class);
        doReturn(mock(ScheduledFuture.class)).when(executor)
            .scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));
        clientUi = mock(ClientUI.class);
        instances = new ProjectXInstances(new OkHttpClient(), new Gson(), configManager, executor, () -> clientUi);
    }

    @After
    public void tearDown() throws IOException
    {
        System.clearProperty("projectx.siteUrl");
        site.shutdown();
    }

    private Runnable heartbeat()
    {
        ArgumentCaptor<Runnable> beat = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).scheduleWithFixedDelay(beat.capture(), anyLong(), anyLong(), any(TimeUnit.class));
        return beat.getValue();
    }

    @Test
    public void heartbeatCarriesTheExtensionBothWays() throws InterruptedException
    {
        site.enqueue(new MockResponse().setBody("{\"ok\":true}"));
        site.enqueue(new MockResponse().setBody("{\"ok\":true,\"commands\":[{\"id\":\"cmabcdefghij1\",\"type\":\"STOP_ALL\"}]}"));
        assertTrue(instances.claim());
        site.takeRequest();
        instances.setExtension(extension);

        heartbeat().run();

        String sent = site.takeRequest().getBody().readUtf8();
        assertTrue(sent.contains("\"action\":\"heartbeat\""));
        assertTrue(sent.contains("\"remote\":{\"v\":1}"));
        assertEquals(1, answers.size());
        assertEquals("cmabcdefghij1", answers.get(0).getAsJsonArray("commands").get(0).getAsJsonObject().get("id").getAsString());
    }

    /** Stop on the website still closes the client with website control installed. */
    @Test
    public void stopStillShutsTheClientDown()
    {
        site.enqueue(new MockResponse().setBody("{\"ok\":true}"));
        site.enqueue(new MockResponse().setResponseCode(409).setBody("{\"error\":\"stopped\"}"));
        assertTrue(instances.claim());
        instances.setExtension(extension);

        heartbeat().run();

        verify(clientUi).requestShutdown();
        assertTrue(answers.isEmpty());
    }

    /** Without anything live the schedule's faster ticks are skipped down to the old rate. */
    @Test
    public void withoutLiveDetailsItBeatsAtTheOldRate() throws InterruptedException
    {
        site.enqueue(new MockResponse().setBody("{\"ok\":true}"));
        site.enqueue(new MockResponse().setBody("{\"ok\":true}"));
        site.enqueue(new MockResponse().setBody("{\"ok\":true}"));
        assertTrue(instances.claim());
        site.takeRequest();
        live = false;
        instances.setExtension(extension);

        Runnable beat = heartbeat();
        beat.run();
        beat.run();

        assertTrue(site.takeRequest(1, TimeUnit.SECONDS).getBody().readUtf8().contains("heartbeat"));
        assertNull(site.takeRequest(200, TimeUnit.MILLISECONDS));

        live = true;
        beat.run();
        assertTrue(site.takeRequest(1, TimeUnit.SECONDS).getBody().readUtf8().contains("heartbeat"));
    }

    /** A broken extension costs its own fields, never the slot. */
    @Test
    public void aBrokenExtensionNeverStopsTheHeartbeat() throws InterruptedException
    {
        site.enqueue(new MockResponse().setBody("{\"ok\":true}"));
        site.enqueue(new MockResponse().setBody("{\"ok\":true}"));
        assertTrue(instances.claim());
        site.takeRequest();
        instances.setExtension(new ProjectXInstances.Extension()
        {
            @Override
            public void addTo(JsonObject body)
            {
                throw new IllegalStateException("no");
            }

            @Override
            public void answered(JsonObject answer)
            {
                throw new IllegalStateException("no");
            }

            @Override
            public boolean wantsLiveHeartbeat()
            {
                return true;
            }
        });

        heartbeat().run();

        String sent = site.takeRequest(1, TimeUnit.SECONDS).getBody().readUtf8();
        assertTrue(sent.contains("\"action\":\"heartbeat\""));
        assertFalse(sent.contains("remote"));
    }
}
