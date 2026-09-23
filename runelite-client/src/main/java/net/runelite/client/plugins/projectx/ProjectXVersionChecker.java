package net.runelite.client.plugins.projectx;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLiteProperties;
import net.runelite.client.plugins.projectx.util.misc.Rs2UiHelper;

import javax.inject.Singleton;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Singleton
public class ProjectXVersionChecker
{
	private static final Pattern TAG_NAME = Pattern.compile("\"tag_name\"\\s*:\\s*\"v?([^\"]+)\"");

	private final AtomicBoolean newVersionAvailable = new AtomicBoolean(false);
	private final AtomicBoolean scheduled = new AtomicBoolean(false);
	private volatile ScheduledFuture<?> future;
	// The newest published client is whatever the latest release is tagged.
	private final String REMOTE_VERSION_URL = "https://api.github.com/repos/iEasyScript/xclient/releases/latest";

	private final boolean disableTelemetry;

	@Inject
	public ProjectXVersionChecker(@Named("disableTelemetry") boolean disableTelemetry) {
		this.disableTelemetry = disableTelemetry;
	}

	private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = new Thread(r, ProjectXVersionChecker.class.getSimpleName());
		t.setDaemon(true);
		t.setUncaughtExceptionHandler((th, e) -> log.warn("Version checker thread error", e));
		return t;
	});

	private void runVersionCheck()
	{
		if (newVersionAvailable.get())
		{
			return;
		}

		try
		{
			String remoteVersion = fetchRemoteVersion();
			String localVersion = RuneLiteProperties.getProjectXVersion();
			String remote = remoteVersion == null ? null : remoteVersion.trim();
			String local = localVersion == null ? "" : localVersion.trim();
			if (remote != null && !remote.isEmpty() && Rs2UiHelper.compareVersions(local, remote) < 0)
			{
				newVersionAvailable.set(true);
				log.info("New ProjectX client version available: {} (current: {})", remote, local);
			}
			else
			{
				log.debug("ProjectX client is up to date: {}", local);
			}
		}
		catch (Exception e)
		{
			log.warn("Could not check ProjectX client version", e);
		}
	}

	private String fetchRemoteVersion() throws Exception
	{
		var url = new URL(REMOTE_VERSION_URL);
		var conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(5_000);
		conn.setReadTimeout(5_000);
		conn.setRequestMethod("GET");
		conn.setInstanceFollowRedirects(true);
		try (var reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8)))
		{
			if (conn.getResponseCode() != 200)
			{
				log.debug("Version check responded with HTTP {}", conn.getResponseCode());
				return null;
			}
			StringBuilder body = new StringBuilder();
			for (String line = reader.readLine(); line != null; line = reader.readLine())
			{
				body.append(line);
			}
			// {"tag_name":"2.6.22", ...} -- the tag is the version.
			Matcher tag = TAG_NAME.matcher(body);
			return tag.find() ? tag.group(1) : null;
		}
		finally
		{
			conn.disconnect();
		}
	}

	public void checkForUpdate()
	{
		if (ProjectX.isDebug()) {
			return;
		}
		if (disableTelemetry || ProjectX.isTelemetryDisabled()) {
			return;
		}

		if (scheduled.compareAndSet(false, true))
		{
			future = scheduledExecutorService.schedule(this::runVersionCheck, 0, TimeUnit.SECONDS);
		}
	}

	public void shutdown()
	{
		try
		{
			if (future != null)
			{
				future.cancel(true);
			}
		}
		finally
		{
			scheduledExecutorService.shutdownNow();
			newVersionAvailable.set(false);
			scheduled.set(false);
		}
	}
}
