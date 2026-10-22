/*
 * Copyright (c) 2024 ProjectX Contributors
 * All rights reserved.
 */
package net.runelite.client.ui;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.joran.spi.ConsoleTarget;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.function.Consumer;

final class ConsoleLogAppender extends AppenderBase<ILoggingEvent>
{
	private final Consumer<String> logConsumer;
	private PatternLayout layout;

	ConsoleLogAppender(Consumer<String> logConsumer)
	{
		this.logConsumer = logConsumer;
	}

	static void bindStdoutAppenders(LoggerContext context, OutputStream stdout)
	{
		OutputStream shielded = new CloseShieldOutputStream(stdout);
		for (Logger logger : context.getLoggerList())
		{
			Iterator<Appender<ILoggingEvent>> appenders = logger.iteratorForAppenders();
			while (appenders.hasNext())
			{
				Appender<ILoggingEvent> appender = appenders.next();
				if (appender instanceof ConsoleAppender
					&& ConsoleTarget.SystemOut.getName().equals(((ConsoleAppender<ILoggingEvent>) appender).getTarget()))
				{
					((ConsoleAppender<ILoggingEvent>) appender).setOutputStream(shielded);
				}
			}
		}
	}

	@Override
	public void start()
	{
		layout = new PatternLayout();
		layout.setContext(getContext());
		layout.setPattern("%d{HH:mm:ss} %-5level %logger{36} - %msg%n");
		layout.start();
		super.start();
	}

	@Override
	protected void append(ILoggingEvent eventObject)
	{
		if (!isStarted())
		{
			return;
		}
		String formatted = layout.doLayout(eventObject);
		logConsumer.accept(formatted);
	}

	private static final class CloseShieldOutputStream extends FilterOutputStream
	{
		private CloseShieldOutputStream(OutputStream out)
		{
			super(out);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException
		{
			out.write(b, off, len);
		}

		@Override
		public void close() throws IOException
		{
			flush();
		}
	}
}
