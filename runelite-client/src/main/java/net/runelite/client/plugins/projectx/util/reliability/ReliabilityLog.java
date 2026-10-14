package net.runelite.client.plugins.projectx.util.reliability;

import java.util.regex.Pattern;

/**
 * Builds the structured diagnostic lines the service writes through the normal logger, so
 * they land in this launch's session log under {@code .runelite/logs/projectx}:
 *
 * <pre>[Reliability] script=Woodcutter step="Chop tree" event=retry attempt=2/4 reason="not confirmed within 3000ms"</pre>
 *
 * Only script, step and failure details go in. Text that came from a script (step names,
 * reasons, exception messages) is flattened to one line, shortened, and has anything that
 * looks like an email address or a long token masked, so a careless reason string cannot
 * leak an account login or session key into a log someone later attaches to a report.
 */
final class ReliabilityLog
{
    static final int MAX_VALUE_LENGTH = 200;

    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+[.][A-Za-z]{2,}");
    /** Long unbroken runs of letters, digits and token punctuation: API tokens, JWTs, session ids. */
    private static final Pattern TOKEN = Pattern.compile("[A-Za-z0-9_=+/-]{32,}");

    private ReliabilityLog()
    {
    }

    static String line(String script, String step, String event, Object... keyValues)
    {
        StringBuilder sb = new StringBuilder("[Reliability] script=").append(quote(script));
        if (step != null)
        {
            sb.append(" step=").append(quote(step));
        }
        sb.append(" event=").append(event);
        for (int i = 0; i + 1 < keyValues.length; i += 2)
        {
            Object value = keyValues[i + 1];
            if (value == null)
            {
                continue;
            }
            sb.append(' ').append(keyValues[i]).append('=');
            sb.append(value instanceof Number || value instanceof Boolean ? value.toString() : quote(value.toString()));
        }
        return sb.toString();
    }

    /** One line, no quotes, nothing that looks like a login or a token, at most 200 characters. */
    static String sanitize(String text)
    {
        if (text == null)
        {
            return "";
        }
        String s = text.replaceAll("[\r\n\t]+", " ").replace('"', '\'').trim();
        s = EMAIL.matcher(s).replaceAll("<email>");
        s = TOKEN.matcher(s).replaceAll("<redacted>");
        if (s.length() > MAX_VALUE_LENGTH)
        {
            s = s.substring(0, MAX_VALUE_LENGTH - 3) + "...";
        }
        return s;
    }

    private static String quote(String text)
    {
        String s = sanitize(text);
        return s.indexOf(' ') < 0 && !s.isEmpty() ? s : '"' + s + '"';
    }

    /** "IllegalStateException: Bank did not open", sanitized. */
    static String describe(Throwable t)
    {
        if (t == null)
        {
            return "";
        }
        String msg = t.getMessage();
        return sanitize(t.getClass().getSimpleName() + (msg == null || msg.isEmpty() ? "" : ": " + msg));
    }
}
