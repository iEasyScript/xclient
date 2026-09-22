/*
 * Copyright (c) 2024 ProjectX Contributors
 * All rights reserved.
 */
package net.runelite.client.ui;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import java.util.function.Consumer;

final class ConsoleLogAppender extends AppenderBase<ILoggingEvent>
{
	private final Consumer<String> logConsumer;
	private PatternLayout layout;

	ConsoleLogAppender(Consumer<String> logConsumer)
	{
		this.logConsumer = logConsumer;
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
}
