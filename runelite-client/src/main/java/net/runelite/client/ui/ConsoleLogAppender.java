/*
 * Copyright (c) 2024 ProjectX Contributors
 * All rights reserved.
 */
package net.runelite.client.ui;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.joran.spi.ConsoleTarget;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.function.Consumer;

final class ConsoleLogAppender extends AppenderBase<ILoggingEvent> implements LoggerContextListener
{
	private final Consumer<String> logConsumer;
	private final OutputStream stdout;
	private PatternLayout layout;
	private volatile boolean rebindPending;

	ConsoleLogAppender(Consumer<String> logConsumer)
	{
		this(logConsumer, null);
	}

	private ConsoleLogAppender(Consumer<String> logConsumer, OutputStream stdout)
	{
		this.logConsumer = logConsumer;
		this.stdout = stdout;
	}

	static ConsoleLogAppender install(LoggerContext context, OutputStream stdout, Consumer<String> logConsumer)
	{
		ConsoleLogAppender appender = new ConsoleLogAppender(logConsumer, stdout);
		appender.setContext(context);
		bindStdoutAppenders(context, stdout);
		appender.start();
		context.addListener(appender);
		context.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(appender);
		return appender;
	}

	void uninstall()
	{
		LoggerContext context = (LoggerContext) getContext();
		context.removeListener(this);
		context.getLogger(Logger.ROOT_LOGGER_NAME).detachAppender(this);
		stop();
	}

	static boolean bindStdoutAppenders(LoggerContext context, OutputStream stdout)
	{
		OutputStream shielded = new CloseShieldOutputStream(stdout);
		boolean bound = false;
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
					bound = true;
				}
			}
		}
		return bound;
	}

	@Override
	public boolean isResetResistant()
	{
		return true;
	}

	@Override
	public void onStart(LoggerContext context)
	{
	}

	@Override
	public void onReset(LoggerContext context)
	{
		rebindPending = stdout != null;
		start();
		context.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(this);
	}

	@Override
	public void onStop(LoggerContext context)
	{
		rebindPending = false;
		context.getLogger(Logger.ROOT_LOGGER_NAME).detachAppender(this);
		stop();
	}

	@Override
	public void onLevelChange(Logger logger, Level level)
	{
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
		if (rebindPending && bindStdoutAppenders((LoggerContext) getContext(), stdout))
		{
			rebindPending = false;
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
