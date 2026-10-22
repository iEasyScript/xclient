package net.runelite.client.config;

import com.google.gson.Gson;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ProfileManagerLockTest
{
	private ProfileManager profileManager;
	private ExecutorService executor;

	@Before
	public void setUp()
	{
		profileManager = new ProfileManager(new Gson());
		executor = Executors.newSingleThreadExecutor();
	}

	@After
	public void tearDown()
	{
		executor.shutdownNow();
	}

	@Test
	public void secondThreadWaitsForHeldLockInsteadOfThrowing() throws Exception
	{
		Future<Boolean> second;
		try (ProfileManager.Lock ignored = profileManager.lock())
		{
			second = executor.submit(() ->
			{
				try (ProfileManager.Lock lock = profileManager.lock())
				{
					return lock.getProfiles() != null;
				}
			});

			try
			{
				second.get(300, TimeUnit.MILLISECONDS);
				fail("second lock acquired while the first was still held");
			}
			catch (TimeoutException expected)
			{
			}
			catch (ExecutionException e)
			{
				throw new AssertionError("second lock threw instead of waiting", e.getCause());
			}
			assertFalse(second.isDone());
		}

		assertTrue(second.get(10, TimeUnit.SECONDS));
	}

	@Test
	public void lockIsReacquirableAfterClose()
	{
		try (ProfileManager.Lock ignored = profileManager.lock())
		{
			assertNotNull(ignored.getProfiles());
		}
		try (ProfileManager.Lock lock = profileManager.lock())
		{
			assertNotNull(lock.getProfiles());
		}
	}
}
