package net.runelite.client.plugins.projectx;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BlockingEventManagerLoggingTest
{
	private Logger logger;
	private Level previousLevel;
	private CountDownLatch logged;
	private ListAppender<ILoggingEvent> appender;
	private BlockingEventManager manager;

	@Before
	public void setUp()
	{
		logger = (Logger) LoggerFactory.getLogger(ProjectX.class);
		previousLevel = logger.getLevel();
		logger.setLevel(Level.DEBUG);
		logged = new CountDownLatch(1);
		appender = new ListAppender<ILoggingEvent>()
		{
			@Override
			protected void append(ILoggingEvent event)
			{
				super.append(event);
				logged.countDown();
			}
		};
		appender.start();
		logger.addAppender(appender);

		manager = new BlockingEventManager();
		for (BlockingEvent event : new ArrayList<>(manager.getEvents()))
		{
			manager.remove(event);
		}
	}

	@After
	public void tearDown()
	{
		manager.shutdown();
		logger.detachAppender(appender);
		logger.setLevel(previousLevel);
	}

	@Test
	public void executeFailureIsLoggedWithThrowable() throws Exception
	{
		manager.add(new FailingEvent(false, new RuntimeException("Timed out waiting for client thread")));

		validateAndEnqueue();
		assertTrue(manager.shouldBlockAndProcess());
		assertTrue(logged.await(5, TimeUnit.SECONDS));

		ILoggingEvent event = appender.list.get(0);
		assertEquals(Level.ERROR, event.getLevel());
		assertEquals("Error executing BlockingEvent (FailingEvent)", event.getFormattedMessage());
		IThrowableProxy throwable = event.getThrowableProxy();
		assertNotNull(throwable);
		assertEquals(RuntimeException.class.getName(), throwable.getClassName());
		assertTrue(throwable.getStackTraceElementProxyArray().length > 0);
	}

	@Test
	public void validateFailureIsLoggedWithThrowable() throws Exception
	{
		manager.add(new FailingEvent(true, new IllegalStateException("boom")));

		validateAndEnqueue();
		assertTrue(logged.await(5, TimeUnit.SECONDS));

		ILoggingEvent event = appender.list.get(0);
		assertEquals(Level.ERROR, event.getLevel());
		assertEquals("Error validating BlockingEvent (FailingEvent)", event.getFormattedMessage());
		assertNotNull(event.getThrowableProxy());
		assertEquals(IllegalStateException.class.getName(), event.getThrowableProxy().getClassName());
	}

	private void validateAndEnqueue() throws Exception
	{
		Method method = BlockingEventManager.class.getDeclaredMethod("validateAndEnqueueWithBackoff");
		method.setAccessible(true);
		method.invoke(manager);
	}

	private static final class FailingEvent implements BlockingEvent
	{
		private final boolean failValidate;
		private final RuntimeException failure;

		private FailingEvent(boolean failValidate, RuntimeException failure)
		{
			this.failValidate = failValidate;
			this.failure = failure;
		}

		@Override
		public boolean validate()
		{
			if (failValidate)
			{
				throw failure;
			}
			return true;
		}

		@Override
		public boolean execute()
		{
			throw failure;
		}

		@Override
		public BlockingEventPriority priority()
		{
			return BlockingEventPriority.NORMAL;
		}
	}
}
