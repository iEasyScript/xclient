/*
 * Copyright (c) 2024 ProjectX Contributors
 * All rights reserved.
 */
package net.runelite.client.ui;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.MatteBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DefaultCaret;
import net.runelite.api.Constants;

final class LogConsolePanel extends JPanel
{
	private static final int MAX_CHARACTERS = 100_000;
	private static final int PREFERRED_HEIGHT = 160;

	private final JTextArea textArea = new JTextArea();

	LogConsolePanel()
	{
		super(new BorderLayout());
		setPreferredSize(new Dimension(Constants.GAME_FIXED_SIZE.width, PREFERRED_HEIGHT));
		setBorder(new MatteBorder(1, 0, 0, 0, ColorScheme.DARKER_GRAY_COLOR));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);

		textArea.setEditable(false);
		textArea.setLineWrap(false);
		textArea.setWrapStyleWord(false);
		textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		textArea.setBackground(ColorScheme.DARK_GRAY_COLOR);
		textArea.setForeground(new Color(0, 255, 70));
		textArea.setBorder(null);

		DefaultCaret caret = (DefaultCaret) textArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setBorder(null);
		add(scrollPane, BorderLayout.CENTER);
	}

	void append(String text)
	{
		if (text == null || text.isEmpty())
		{
			return;
		}

		String sanitized = text.replace("\r", "");
		if (SwingUtilities.isEventDispatchThread())
		{
			appendOnEdt(sanitized);
		}
		else
		{
			SwingUtilities.invokeLater(() -> appendOnEdt(sanitized));
		}
	}

	void clear()
	{
		if (SwingUtilities.isEventDispatchThread())
		{
			textArea.setText("");
		}
		else
		{
			SwingUtilities.invokeLater(() -> textArea.setText(""));
		}
	}

	OutputStream createOutputStream()
	{
		return new ConsoleOutputStream();
	}

	private void appendOnEdt(String text)
	{
		Document document = textArea.getDocument();
		try
		{
			int length = document.getLength();
			document.insertString(length, text, null);
			trimIfNecessary();
			textArea.setCaretPosition(document.getLength());
		}
		catch (BadLocationException ex)
		{
			// Ignore append failures to avoid recursive logging.
		}
	}

	private void trimIfNecessary()
	{
		Document document = textArea.getDocument();
		int excess = document.getLength() - MAX_CHARACTERS;
		if (excess <= 0)
		{
			return;
		}

		try
		{
			document.remove(0, excess);
		}
		catch (BadLocationException ex)
		{
			// Ignore trim failures to avoid recursive logging.
		}
	}

	private final class ConsoleOutputStream extends OutputStream
	{
		private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		@Override
		public synchronized void write(int b) throws IOException
		{
			if (b == '\r')
			{
				return;
			}

			buffer.write(b);
			if (b == '\n')
			{
				flushBuffer();
			}
		}

		@Override
		public synchronized void write(byte[] b, int off, int len) throws IOException
		{
			for (int i = 0; i < len; i++)
			{
				write(b[off + i]);
			}
		}

		@Override
		public synchronized void flush() throws IOException
		{
			flushBuffer();
		}

		@Override
		public void close() throws IOException
		{
			flush();
		}

		private void flushBuffer() throws IOException
		{
			if (buffer.size() == 0)
			{
				return;
			}

			String value = buffer.toString(StandardCharsets.UTF_8);
			buffer.reset();
			append(value);
		}
	}
}
