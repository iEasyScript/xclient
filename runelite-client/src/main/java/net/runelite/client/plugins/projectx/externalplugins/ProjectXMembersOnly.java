package net.runelite.client.plugins.projectx.externalplugins;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginInstantiationException;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.projectx.ProjectX;

/**
 * Keeps a built-in feature for Project X members (any tier) and admins.
 *
 * <p>A plugin calls {@link #watch} from {@code startUp()} and cancels the returned
 * {@link Watch} in {@code shutDown()}. The membership is checked off the client
 * thread and the EDT -- {@link ProjectXAccount#refreshIfStale()} goes to the network --
 * once straight away and again every few minutes. While it holds, the feature's
 * start callback runs once, on the EDT. When it does not, the player is told once,
 * and the plugin is switched off and stopped on the EDT, the same way access-ended
 * store scripts are stopped.
 *
 * <p>No identity (signed out, or the site never answered) counts as not a member.
 * A member whose site goes quiet mid-session keeps the last answer, because
 * {@link ProjectXAccount} keeps the previous identity when the site is unreachable.
 *
 * <p>Not a security boundary: the client runs on the user's machine. It decides
 * what the client offers, like {@link ProjectXAccount#isDeveloper()}.
 */
@Slf4j
@Singleton
public class ProjectXMembersOnly
{
	/** How often a running feature re-checks the membership. */
	static final long RECHECK_MINUTES = 5;

	/**
	 * With a token but no answer yet, how often and how long to wait before
	 * giving up: a member opening the client while the site restarts should not
	 * be turned away at the first failed request.
	 */
	static final long UNRESOLVED_RETRY_SECONDS = 15;
	static final int UNRESOLVED_RETRIES = 2;

	/** The same feature is not announced more often than this, whatever the reason. */
	static final long MESSAGE_COOLDOWN_MILLIS = TimeUnit.SECONDS.toMillis(60);

	private final ProjectXAccount account;
	private final ScheduledExecutorService executor;
	private final PluginManager pluginManager;
	private final Map<String, Long> lastToldMillis = new ConcurrentHashMap<>();

	@Inject
	ProjectXMembersOnly(ProjectXAccount account, ScheduledExecutorService executor, PluginManager pluginManager)
	{
		this.account = account;
		this.executor = executor;
		this.pluginManager = pluginManager;
	}

	/** True for a live membership of any tier, or an admin. Null (no identity) is false. */
	public static boolean isAllowed(@Nullable ProjectXAccount.Identity identity)
	{
		return identity != null
			&& (identity.membership() != null || identity.role() == ProjectXAccount.Role.ADMIN);
	}

	/** What a non-member is told when they turn {@code featureName} on. */
	public static String deniedMessage(String featureName)
	{
		return featureName + " is for Project X members (any tier). Get a membership at xclient.dev/membership.";
	}

	/**
	 * Asks the site (at most once a minute) whether the signed-in user may use members' features.
	 * Network I/O: never call on the client thread or the EDT.
	 */
	public boolean checkNow()
	{
		return isAllowed(account.refreshIfStale());
	}

	/**
	 * Starts watching the membership for one plugin. Call from {@code startUp()};
	 * returns at once.
	 *
	 * @param plugin      the plugin to switch off when the user is not a member
	 * @param featureName what the player is told the feature is called
	 * @param onAllowed   runs once on the EDT, the first time membership is confirmed,
	 *                    unless the watch was cancelled first
	 */
	public Watch watch(Plugin plugin, String featureName, Runnable onAllowed)
	{
		Watch watch = new Watch(plugin, featureName, onAllowed);
		watch.schedule(0, TimeUnit.SECONDS);
		return watch;
	}

	/** One plugin's membership watch. Cancel it from {@code shutDown()}. */
	public final class Watch
	{
		private final Plugin plugin;
		private final String featureName;
		private final Runnable onAllowed;
		private final AtomicBoolean cancelled = new AtomicBoolean();
		private final AtomicBoolean confirmed = new AtomicBoolean();
		private int retriesLeft = UNRESOLVED_RETRIES;
		private volatile ScheduledFuture<?> future;

		private Watch(Plugin plugin, String featureName, Runnable onAllowed)
		{
			this.plugin = plugin;
			this.featureName = featureName;
			this.onAllowed = onAllowed;
		}

		/** Stops further checks; a pending start callback is dropped. Safe from any thread, any number of times. */
		public void cancel()
		{
			cancelled.set(true);
			ScheduledFuture<?> f = future;
			if (f != null)
			{
				f.cancel(false);
			}
		}

		public boolean isCancelled()
		{
			return cancelled.get();
		}

		private void schedule(long delay, TimeUnit unit)
		{
			if (!cancelled.get())
			{
				future = executor.schedule(this::check, delay, unit);
			}
		}

		/** Runs on the executor, never on the client thread or the EDT. */
		private void check()
		{
			if (cancelled.get())
			{
				return;
			}

			ProjectXAccount.Identity identity;
			try
			{
				identity = account.refreshIfStale();
			}
			catch (Exception e)
			{
				log.debug("Membership check for {} failed: {}", featureName, e.getMessage());
				identity = account.getIdentity();
			}

			if (isAllowed(identity))
			{
				if (confirmed.compareAndSet(false, true))
				{
					SwingUtilities.invokeLater(() ->
					{
						if (!cancelled.get())
						{
							onAllowed.run();
						}
					});
				}
				schedule(RECHECK_MINUTES, TimeUnit.MINUTES);
				return;
			}

			boolean unresolved = identity == null && account.currentToken() != null;
			if (unresolved && !confirmed.get() && retriesLeft > 0)
			{
				retriesLeft--;
				schedule(UNRESOLVED_RETRY_SECONDS, TimeUnit.SECONDS);
				return;
			}

			deny(identity == null);
		}

		private void deny(boolean noIdentity)
		{
			if (!cancelled.compareAndSet(false, true))
			{
				return;
			}

			log.info("{} is for Project X members; {}. Turning it off.", featureName,
				noIdentity ? "not signed in, or Project X could not be reached" : "no live membership");

			long now = System.currentTimeMillis();
			Long last = lastToldMillis.get(featureName);
			if (last == null || now - last >= MESSAGE_COOLDOWN_MILLIS)
			{
				lastToldMillis.put(featureName, now);
				ProjectX.showMessage(deniedMessage(featureName));
			}

			if (confirmed.get())
			{
				// It was running: record why, so the stop is not reported as a crash.
				ProjectXSessionTracker.markStop(plugin, ProjectXSessionTracker.End.ACCESS_ENDED);
			}

			// Plugins start and stop on the EDT. invokeLater, not invokeAndWait: this
			// thread is the shared executor, and nothing here needs to wait for the stop.
			SwingUtilities.invokeLater(() ->
			{
				try
				{
					pluginManager.setPluginEnabled(plugin, false);
					pluginManager.stopPlugin(plugin);
				}
				catch (PluginInstantiationException e)
				{
					log.warn("Unable to stop {}", plugin.getClass().getSimpleName(), e);
				}
			});
		}
	}
}
