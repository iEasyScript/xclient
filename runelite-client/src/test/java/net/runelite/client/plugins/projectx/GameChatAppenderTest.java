package net.runelite.client.plugins.projectx;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import java.lang.reflect.Field;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GameChatAppenderTest
{
	private Client client;
	private ClientThread clientThread;
	private Object previousClient;
	private Object previousClientThread;
	private GameChatAppender appender;

	@Before
	public void setUp() throws Exception
	{
		client = mock(Client.class);
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(client.isClientThread()).thenReturn(false);
		clientThread = mock(ClientThread.class);
		previousClient = swapStatic("client", client);
		previousClientThread = swapStatic("clientThread", clientThread);

		GameChatAppender.updateConfiguration(true, Level.WARN, true);
		appender = new GameChatAppender();
		appender.setContext(new LoggerContext());
		appender.start();
	}

	@After
	public void tearDown() throws Exception
	{
		appender.stop();
		swapStatic("client", previousClient);
		swapStatic("clientThread", previousClientThread);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void appendQueuesChatMessageWithoutWaitingForClientThread()
	{
		LoggerContext context = new LoggerContext();
		LoggingEvent event = new LoggingEvent(
			"test",
			context.getLogger("net.runelite.client.plugins.projectx.walker"),
			Level.WARN,
			"walker stalled",
			null,
			null);

		appender.doAppend(event);

		ArgumentCaptor<Runnable> queued = ArgumentCaptor.forClass(Runnable.class);
		verify(clientThread).invoke(queued.capture());
		verify(clientThread, never()).invoke(any(Supplier.class));
		verify(clientThread, never()).invoke(any(BooleanSupplier.class));

		queued.getValue().run();
		ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
		verify(client).addChatMessage(eq(ChatMessageType.ENGINE), eq(""), message.capture(), eq(""), anyBoolean());
		assertTrue(message.getValue().contains("walker stalled"));
	}

	@Test
	public void filteredEventDoesNotTouchClientThread()
	{
		LoggerContext context = new LoggerContext();
		LoggingEvent event = new LoggingEvent(
			"test",
			context.getLogger("net.runelite.client.plugins.projectx.walker"),
			Level.DEBUG,
			"route detail",
			null,
			null);

		appender.doAppend(event);

		verify(clientThread, never()).invoke(any(Runnable.class));
		verify(client, never()).addChatMessage(any(), anyString(), anyString(), anyString(), anyBoolean());
	}

	private static Object swapStatic(String name, Object value) throws Exception
	{
		Field field = ProjectX.class.getDeclaredField(name);
		field.setAccessible(true);
		Object previous = field.get(null);
		field.set(null, value);
		return previous;
	}
}
