package net.runelite.client.plugins.projectx.externalplugins;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.projectx.ProjectXConfig;
import net.runelite.client.ui.ClientUI;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * How many clients this account may run at once.
 *
 * Every account gets one instance; more are rented or bought on the website.
 * The count is kept by the site rather than the client, so it holds when
 * somebody runs clients on several machines.
 *
 * <p>A client claims a slot before plugins start, heartbeats while it runs, and
 * releases the slot on exit. A client that dies without releasing stops
 * heartbeating, and the site frees the slot shortly after.
 */
@Slf4j
@Singleton
public class ProjectXInstances
{
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final long HEARTBEAT_SECONDS = 30;

    /**
     * The one refusal a client must obey.
     *
     * A 409 usually means "no slot free", which a running client is right to
     * carry on through. This one means somebody pressed Stop on the website, and
     * it is deliberately a different answer so the two cannot be confused.
     */
    private static final String STOPPED = "stopped";

    /** One id per launch: two clients on one machine are two instances. */
    private final String sessionId = UUID.randomUUID().toString().replace("-", "");

    private final OkHttpClient okHttpClient;
    private final Gson gson;
    private final ConfigManager configManager;
    private final ScheduledExecutorService executor;

    /**
     * Lazily: this is built before the window is, and asking for the UI outright
     * would drag it into existence at the wrong point in startup.
     */
    private final Provider<ClientUI> clientUi;

    /** Why the claim failed, for the message shown to the user. */
    @Getter
    private volatile String refusal;

    private volatile ScheduledFuture<?> heartbeat;

    @Inject
    ProjectXInstances(
            OkHttpClient okHttpClient,
            Gson gson,
            ConfigManager configManager,
            ScheduledExecutorService executor,
            Provider<ClientUI> clientUi)
    {
        this.okHttpClient = okHttpClient;
        this.gson = gson;
        this.configManager = configManager;
        this.executor = executor;
        this.clientUi = clientUi;
    }

    /**
     * Takes a slot for this client. Returns false when the client must not run:
     * no token, no slot free, or the site could not be reached.
     */
    public boolean claim()
    {
        String token = token();
        if (Strings.isNullOrEmpty(token))
        {
            refusal = "Project X needs your account to start.\n\n"
                    + "Create an API token on " + ProjectXSite.baseUrl() + "account\n"
                    + "and paste it into Project X settings -> Project X account -> API token.";
            return false;
        }

        JsonObject body = new JsonObject();
        body.addProperty("action", "claim");
        body.addProperty("sessionId", sessionId);
        body.addProperty("label", machineName());

        try (Response response = post(body, token))
        {
            if (response.code() == 401)
            {
                refusal = "Your Project X API token was not accepted.\n\n"
                        + "Create a new one on " + ProjectXSite.baseUrl() + "account";
                return false;
            }

            if (response.code() == 409)
            {
                refusal = describeShortfall(response) + "\n\n"
                        + "Close a client, or add an instance at " + ProjectXSite.baseUrl() + "instances";
                return false;
            }

            if (!response.isSuccessful())
            {
                refusal = "Could not check your instances: the site answered HTTP " + response.code() + ".";
                return false;
            }

            log.info("Instance slot claimed ({})", sessionId);
            heartbeat = executor.scheduleWithFixedDelay(this::beat, HEARTBEAT_SECONDS, HEARTBEAT_SECONDS, TimeUnit.SECONDS);
            return true;
        }
        catch (IOException e)
        {
            // Fail closed: an unreachable site must not become a way to run
            // unlimited clients. Local development is the exception -- the site
            // then IS this machine, and nobody is being cheated.
            if (isLocalSite())
            {
                log.warn("Instance check skipped: {} is not running", ProjectXSite.baseUrl());
                return true;
            }
            refusal = "Could not reach " + ProjectXSite.baseUrl() + " to check your instances.\n\n"
                    + "Check your connection and start the client again.";
            return false;
        }
    }

    /** Gives the slot back. Best effort: the site frees it anyway once heartbeats stop. */
    public void release()
    {
        String token = token();
        if (Strings.isNullOrEmpty(token))
        {
            return;
        }

        JsonObject body = new JsonObject();
        body.addProperty("action", "release");
        body.addProperty("sessionId", sessionId);

        try (Response ignored = post(body, token))
        {
            log.debug("Instance slot released ({})", sessionId);
        }
        catch (IOException e)
        {
            log.debug("Could not release instance slot: {}", e.getMessage());
        }
    }

    private void beat()
    {
        String token = token();
        if (Strings.isNullOrEmpty(token))
        {
            return;
        }

        JsonObject body = new JsonObject();
        body.addProperty("action", "heartbeat");
        body.addProperty("sessionId", sessionId);

        try (Response response = post(body, token))
        {
            if (response.code() != 409)
            {
                return;
            }

            // Read once: the body is a stream, and both branches below want it.
            JsonObject answer = payload(response);

            if (STOPPED.equals(string(answer, "error")))
            {
                stop(string(answer, "message"));
                return;
            }

            // The slot went rather than this client being stopped -- a rental
            // lapsed, most likely. Keep running; the user is mid-session and
            // pulling the rug from under them helps nobody.
            log.warn("This client no longer holds an instance slot: {}", shortfall(answer));
        }
        catch (IOException e)
        {
            log.debug("Instance heartbeat failed: {}", e.getMessage());
        }
    }

    /**
     * Closes the client, because the website asked it to.
     *
     * The site drops the session as it answers, so the slot is already free by
     * the time this runs. The heartbeat is cancelled first: shutting down takes a
     * moment, and a heartbeat landing during it would claim a fresh slot and put
     * this client straight back on the list it was just removed from.
     */
    private void stop(String message)
    {
        log.info("Shutting down: {}", Strings.isNullOrEmpty(message) ? "stopped from the website" : message);

        ScheduledFuture<?> beating = heartbeat;
        if (beating != null)
        {
            beating.cancel(false);
        }

        clientUi.get().requestShutdown();
    }

    private Response post(JsonObject body, String token) throws IOException
    {
        Request request = new Request.Builder()
                .url(ProjectXSite.api("instances"))
                .header("Authorization", "Bearer " + token.trim())
                .post(RequestBody.create(JSON, gson.toJson(body)))
                .build();

        return okHttpClient.newCall(request).execute();
    }

    private String describeShortfall(Response response)
    {
        return shortfall(payload(response));
    }

    private String shortfall(JsonObject answer)
    {
        String message = string(answer, "message");
        return Strings.isNullOrEmpty(message) ? "You have no instances free." : message;
    }

    /** The response body, or null if there was not one that could be read. */
    private JsonObject payload(Response response)
    {
        try
        {
            return gson.fromJson(response.body().string(), JsonObject.class);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private static String string(JsonObject json, String key)
    {
        if (json == null || !json.has(key) || !json.get(key).isJsonPrimitive())
        {
            return null;
        }
        return json.get(key).getAsString();
    }

    private String token()
    {
        return ProjectXAccount.token(configManager);
    }

    private boolean isLocalSite()
    {
        String host = ProjectXSite.baseUrl().host();
        return host.equals("localhost") || host.equals("127.0.0.1") || host.equals("::1");
    }

    private static String machineName()
    {
        try
        {
            return InetAddress.getLocalHost().getHostName();
        }
        catch (Exception e)
        {
            return System.getProperty("user.name", "Project X client");
        }
    }
}
