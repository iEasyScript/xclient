package net.runelite.client.plugins.projectx.agentserver.mcp;

import net.runelite.api.events.ChatMessage;
import net.runelite.client.util.Text;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The last chat messages the client received while the MCP server's plugin was running, in order,
 * each with a sequence number so waits can ask for "anything after N".
 */
public final class ChatLog
{
	public static final int CAPACITY = 500;

	public static final class Entry
	{
		public final long seq;
		public final long timeMs;
		public final String type;
		public final String name;
		public final String sender;
		public final String message;

		Entry(long seq, long timeMs, String type, String name, String sender, String message)
		{
			this.seq = seq;
			this.timeMs = timeMs;
			this.type = type;
			this.name = name;
			this.sender = sender;
			this.message = message;
		}

		public Map<String, Object> toMap()
		{
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("seq", seq);
			m.put("time", java.time.Instant.ofEpochMilli(timeMs).toString());
			m.put("type", type);
			if (name != null && !name.isEmpty())
			{
				m.put("name", name);
			}
			if (sender != null && !sender.isEmpty())
			{
				m.put("sender", sender);
			}
			m.put("message", message);
			return m;
		}
	}

	private final Deque<Entry> entries = new ArrayDeque<>();
	private long nextSeq = 1;

	public void record(ChatMessage event)
	{
		if (event == null)
		{
			return;
		}
		record(event.getType() != null ? event.getType().name() : "UNKNOWN",
			event.getName() != null ? Text.removeTags(event.getName()) : null,
			event.getSender(),
			event.getMessage() != null ? Text.removeTags(event.getMessage()) : "");
	}

	public synchronized void record(String type, String name, String sender, String message)
	{
		entries.addLast(new Entry(nextSeq++, System.currentTimeMillis(), type, name, sender, message));
		while (entries.size() > CAPACITY)
		{
			entries.removeFirst();
		}
		notifyAll();
	}

	/** Sequence number of the newest message, 0 when none yet. */
	public synchronized long lastSeq()
	{
		return nextSeq - 1;
	}

	/** Messages with seq greater than {@code afterSeq}, oldest first. */
	public synchronized List<Entry> after(long afterSeq)
	{
		List<Entry> out = new ArrayList<>();
		for (Entry e : entries)
		{
			if (e.seq > afterSeq)
			{
				out.add(e);
			}
		}
		return out;
	}

	/** Waits up to {@code ms} for a new message (or returns early when one arrives). */
	public synchronized void awaitNew(long afterSeq, long ms) throws InterruptedException
	{
		if (lastSeq() > afterSeq || ms <= 0)
		{
			return;
		}
		wait(ms);
	}
}
