package net.runelite.client.ui;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ConsoleLogAppenderTest
{
	private PrintStream originalOut;
	private LoggerContext context;

	@Before
	public void setUp()
	{
		originalOut = System.out;
		context = new LoggerContext();
	}

	@After
	public void tearDown()
	{
		System.setOut(originalOut);
		context.stop();
	}

	@Test
	public void logLineReachesTheConsoleOnceAndStdoutOnce()
	{
		ByteArrayOutputStream terminal = new ByteArrayOutputStream();
		ByteArrayOutputStream consolePanel = new ByteArrayOutputStream();
		StringBuilder console = new StringBuilder();

		Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);
		PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		encoder.setContext(context);
		encoder.setPattern("%msg%n");
		encoder.start();
		ConsoleAppender<ILoggingEvent> stdout = new ConsoleAppender<>();
		stdout.setContext(context);
		stdout.setEncoder(encoder);
		stdout.start();
		root.addAppender(stdout);

		System.setOut(new PrintStream(new Tee(consolePanel, terminal), true, StandardCharsets.UTF_8));

		ConsoleLogAppender.bindStdoutAppenders(context, terminal);
		ConsoleLogAppender appender = new ConsoleLogAppender(console::append);
		appender.setContext(context);
		appender.start();
		root.addAppender(appender);

		root.info("flood line");
		System.out.println("plain print");

		String panel = console + consolePanel.toString(StandardCharsets.UTF_8);
		assertEquals(1, occurrences(panel, "flood line"));
		assertEquals(1, occurrences(panel, "plain print"));

		String out = terminal.toString(StandardCharsets.UTF_8);
		assertEquals(1, occurrences(out, "flood line"));
		assertEquals(1, occurrences(out, "plain print"));
	}

	private static int occurrences(String text, String needle)
	{
		int count = 0;
		for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + 1))
		{
			count++;
		}
		return count;
	}

	private static final class Tee extends OutputStream
	{
		private final OutputStream first;
		private final OutputStream second;

		private Tee(OutputStream first, OutputStream second)
		{
			this.first = first;
			this.second = second;
		}

		@Override
		public void write(int b) throws IOException
		{
			first.write(b);
			second.write(b);
		}
	}
}
