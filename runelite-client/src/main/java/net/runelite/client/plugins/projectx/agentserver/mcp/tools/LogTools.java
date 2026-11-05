package net.runelite.client.plugins.projectx.agentserver.mcp.tools;

import net.runelite.client.plugins.projectx.ProjectXSessionLog;
import net.runelite.client.plugins.projectx.agentserver.mcp.ChatLog;
import net.runelite.client.plugins.projectx.agentserver.mcp.McpContext;
import net.runelite.client.plugins.projectx.agentserver.mcp.McpError;
import net.runelite.client.plugins.projectx.agentserver.mcp.McpToolRegistry;
import net.runelite.client.plugins.projectx.agentserver.mcp.Schema;
import net.runelite.client.plugins.projectx.agentserver.mcp.ToolArgs;
import net.runelite.client.plugins.projectx.agentserver.mcp.ToolFailure;
import net.runelite.client.plugins.projectx.agentserver.mcp.ToolResult;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Recent chat, and the client's own session log. */
final class LogTools
{
	/** Bytes read from the end of the log; enough for thousands of lines. */
	private static final int TAIL_BYTES = 4 * 1024 * 1024;
	private static final Pattern ENTRY_START = Pattern.compile(
		"^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3} \\[[^\\]]*\\] (TRACE|DEBUG|INFO|WARN|ERROR)\\b");
	private static final List<String> LEVELS = Arrays.asList("TRACE", "DEBUG", "INFO", "WARN", "ERROR");

	private LogTools()
	{
	}

	static void register(McpToolRegistry r, McpContext ctx)
	{
		r.category("chat");

		r.add("get_chat_messages",
			"Recent chat messages (game, public, private, clan, ...) received since the MCP server started, oldest "
				+ "first: seq, time, type, name, message. Filter by type (e.g. GAMEMESSAGE), regex pattern, or after_seq "
				+ "(only newer than a seq you saw).",
			Schema.object().integer("limit", "Max messages, newest kept (default 50, max 500)").str("type", "ChatMessageType name")
				.str("pattern", "Regex the message must contain (case-insensitive)").integer("after_seq", "Only messages after this seq")
				.build(),
			a -> ToolResult.json(chat(ctx.chatLog(), a)));

		r.category("logs");

		r.add("read_logs",
			"Tail of this client's session log (~/.runelite/logs/projectx/session_*.log). Entries are whole log "
				+ "records (a stack trace stays with its line). Filter by minimum level (DEBUG/INFO/WARN/ERROR) and a "
				+ "regex; lines = max entries returned (default 100, max 2000). file picks an older session by name.",
			Schema.object().integer("lines", "Max entries (default 100, max 2000)")
				.enumStr("level", "Minimum level", "TRACE", "DEBUG", "INFO", "WARN", "ERROR")
				.str("pattern", "Regex an entry must contain (case-insensitive)")
				.str("file", "A session log file name from get_session_log_path (default: this session)").build(),
			a -> ToolResult.json(readLogs(a)));

		r.add("get_session_log_path",
			"Where this client logs: the current session file, the folder, and the most recent session files with sizes.",
			Schema.empty(),
			a -> ToolResult.json(logPaths()));
	}

	private static Map<String, Object> chat(ChatLog log, ToolArgs a)
	{
		int limit = a.integer("limit", 50, 1, ChatLog.CAPACITY);
		String type = a.str("type");
		Pattern pattern = compile(a.str("pattern"));
		long after = Math.max(0, a.integer("after_seq", 0));
		List<Map<String, Object>> items = new ArrayList<>();
		for (ChatLog.Entry e : log.after(after))
		{
			if (type != null && !type.isEmpty() && !type.equalsIgnoreCase(e.type))
			{
				continue;
			}
			if (pattern != null && !pattern.matcher(e.message).find())
			{
				continue;
			}
			items.add(e.toMap());
		}
		int total = items.size();
		if (items.size() > limit)
		{
			items = new ArrayList<>(items.subList(items.size() - limit, items.size()));
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("count", items.size());
		out.put("matched", total);
		out.put("last_seq", log.lastSeq());
		out.put("items", items);
		return out;
	}

	private static Pattern compile(String regex)
	{
		if (regex == null || regex.isEmpty())
		{
			return null;
		}
		try
		{
			return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		}
		catch (PatternSyntaxException e)
		{
			throw McpError.invalidParams("Bad regex: " + e.getDescription());
		}
	}

	private static File resolveLog(String name)
	{
		if (name == null || name.isEmpty())
		{
			File current = ProjectXSessionLog.current();
			if (current == null)
			{
				throw new ToolFailure("The session log is not running in this client");
			}
			return current;
		}
		if (!name.matches("session_[A-Za-z0-9_.\\-]+\\.log") || name.contains(".."))
		{
			throw McpError.invalidParams("file must be a session_*.log name from get_session_log_path");
		}
		File f = new File(ProjectXSessionLog.DIR, name);
		if (!f.isFile())
		{
			throw new ToolFailure("No such log file: " + name);
		}
		return f;
	}

	private static Map<String, Object> readLogs(ToolArgs a) throws IOException
	{
		int max = a.integer("lines", 100, 1, 2000);
		String level = a.str("level");
		int minLevel = level == null ? 0 : LEVELS.indexOf(level.toUpperCase());
		if (minLevel < 0)
		{
			throw McpError.invalidParams("level must be one of " + LEVELS);
		}
		Pattern pattern = compile(a.str("pattern"));
		File file = resolveLog(a.str("file"));

		String text;
		long size;
		try (RandomAccessFile raf = new RandomAccessFile(file, "r"))
		{
			size = raf.length();
			long from = Math.max(0, size - TAIL_BYTES);
			byte[] buf = new byte[(int) (size - from)];
			raf.seek(from);
			raf.readFully(buf);
			text = new String(buf, StandardCharsets.UTF_8);
			if (from > 0)
			{
				int nl = text.indexOf('\n');
				text = nl >= 0 ? text.substring(nl + 1) : text;
			}
		}

		List<String> entries = new ArrayList<>();
		List<Integer> levels = new ArrayList<>();
		StringBuilder cur = null;
		int curLevel = 0;
		for (String line : text.split("\r?\n"))
		{
			Matcher m = ENTRY_START.matcher(line);
			if (m.find())
			{
				if (cur != null)
				{
					entries.add(cur.toString());
					levels.add(curLevel);
				}
				cur = new StringBuilder(line);
				curLevel = LEVELS.indexOf(m.group(1));
			}
			else if (cur != null)
			{
				cur.append('\n').append(line);
			}
		}
		if (cur != null)
		{
			entries.add(cur.toString());
			levels.add(curLevel);
		}

		List<String> kept = new ArrayList<>();
		for (int i = entries.size() - 1; i >= 0 && kept.size() < max; i--)
		{
			if (levels.get(i) < minLevel)
			{
				continue;
			}
			if (pattern != null && !pattern.matcher(entries.get(i)).find())
			{
				continue;
			}
			kept.add(entries.get(i));
		}
		java.util.Collections.reverse(kept);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("file", file.getAbsolutePath());
		out.put("size", size);
		out.put("scanned_entries", entries.size());
		out.put("returned", kept.size());
		out.put("lines", kept);
		return out;
	}

	private static Map<String, Object> logPaths()
	{
		File current = ProjectXSessionLog.current();
		File dir = ProjectXSessionLog.DIR;
		File[] files = dir.listFiles((d, n) -> n.startsWith("session_") && n.endsWith(".log"));
		List<Map<String, Object>> recent = new ArrayList<>();
		if (files != null)
		{
			Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
			for (int i = 0; i < Math.min(10, files.length); i++)
			{
				File f = files[i];
				recent.add(GameTools.map("name", f.getName(), "size", f.length(),
					"modified", Instant.ofEpochMilli(f.lastModified()).toString()));
			}
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("current", current != null ? current.getAbsolutePath() : null);
		out.put("dir", dir.getAbsolutePath());
		out.put("recent", recent);
		return out;
	}
}
