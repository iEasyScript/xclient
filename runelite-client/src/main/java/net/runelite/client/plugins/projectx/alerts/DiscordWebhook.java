package net.runelite.client.plugins.projectx.alerts;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Posts alerts to the player's own Discord webhook.
 *
 * <p>The webhook URL is a credential: anyone holding it can post to the channel.
 * It is never logged, never put in an exception message, and only ever sent to
 * Discord over https. Logs say "Discord webhook" and the HTTP status, nothing more.
 */
@Slf4j
final class DiscordWebhook
{
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    /** Discord's own hosts. ptb and canary are where webhooks made in those Discord builds point. */
    private static final Set<String> HOSTS = Set.of("discord.com", "ptb.discord.com", "canary.discord.com", "discordapp.com");
    private static final Pattern ID = Pattern.compile("\\d{5,25}");
    private static final Pattern TOKEN = Pattern.compile("[A-Za-z0-9_-]{20,100}");
    /** Discord's embed limits. */
    private static final int TITLE_MAX = 256;
    private static final int DESCRIPTION_MAX = 4096;
    private static final int FIELD_NAME_MAX = 256;
    private static final int FIELD_VALUE_MAX = 1024;
    private static final int FIELDS_MAX = 25;
    /** Characters Discord reads as markdown: shown as typed instead. */
    private static final Pattern MARKDOWN = Pattern.compile("([\\\\*_~`|>#\\[\\]])");
    private static final String ZERO_WIDTH_SPACE = "​";

    private DiscordWebhook()
    {
    }

    /**
     * The webhook URL, if it is one: {@code https://discord.com/api/webhooks/<id>/<token>}
     * (or discordapp.com, ptb./canary.discord.com), nothing else in it but an optional
     * {@code thread_id}. Null for anything else, including http://.
     */
    static HttpUrl parse(String raw)
    {
        if (raw == null)
        {
            return null;
        }
        String trimmed = raw.trim();
        // HttpUrl lowercases and accepts a lot; check the scheme as typed so "HTTP://" is refused too.
        if (!trimmed.regionMatches(true, 0, "https://", 0, "https://".length()))
        {
            return null;
        }
        HttpUrl url = HttpUrl.parse(trimmed);
        if (url == null || !url.isHttps() || !HOSTS.contains(url.host()) || url.port() != 443
            || !url.username().isEmpty() || !url.password().isEmpty() || url.fragment() != null)
        {
            return null;
        }
        java.util.List<String> path = url.pathSegments();
        if (path.size() != 4 || !"api".equals(path.get(0)) || !"webhooks".equals(path.get(1))
            || !ID.matcher(path.get(2)).matches() || !TOKEN.matcher(path.get(3)).matches())
        {
            return null;
        }
        for (String name : url.queryParameterNames())
        {
            String value = url.queryParameter(name);
            if (!"thread_id".equals(name) || value == null || !ID.matcher(value).matches())
            {
                return null;
            }
        }
        return url;
    }

    static boolean isValid(String raw)
    {
        return parse(raw) != null;
    }

    /** The JSON Discord takes: one embed, no mentions of anyone. */
    static String buildPayload(Alert alert)
    {
        JsonObject embed = new JsonObject();
        embed.addProperty("title", truncate(escape(alert.getTitle()), TITLE_MAX));
        if (!alert.getDescription().isEmpty())
        {
            embed.addProperty("description", truncate(escape(alert.getDescription()), DESCRIPTION_MAX));
        }
        embed.addProperty("color", alert.getType().getColor() & 0xFFFFFF);

        JsonArray fields = new JsonArray();
        if (alert.getPlayer() != null && !alert.getPlayer().isEmpty())
        {
            fields.add(field("Player", alert.getPlayer(), true));
        }
        for (Alert.Field f : alert.getFields())
        {
            if (fields.size() >= FIELDS_MAX)
            {
                break;
            }
            if (f.getName() == null || f.getName().isEmpty() || f.getValue() == null || f.getValue().isEmpty())
            {
                continue;
            }
            fields.add(field(f.getName(), f.getValue(), f.isInline()));
        }
        if (fields.size() > 0)
        {
            embed.add("fields", fields);
        }
        JsonObject footer = new JsonObject();
        footer.addProperty("text", "Project X alerts");
        embed.add("footer", footer);
        embed.addProperty("timestamp", Instant.ofEpochMilli(alert.getAt()).toString());

        JsonArray embeds = new JsonArray();
        embeds.add(embed);

        // Nothing in an alert may ping anyone, whatever the text says.
        JsonObject allowedMentions = new JsonObject();
        allowedMentions.add("parse", new JsonArray());

        JsonObject payload = new JsonObject();
        payload.addProperty("username", "Project X");
        payload.add("embeds", embeds);
        payload.add("allowed_mentions", allowedMentions);
        return payload.toString();
    }

    private static JsonObject field(String name, String value, boolean inline)
    {
        JsonObject f = new JsonObject();
        f.addProperty("name", truncate(escape(name), FIELD_NAME_MAX));
        f.addProperty("value", truncate(escape(value), FIELD_VALUE_MAX));
        f.addProperty("inline", inline);
        return f;
    }

    /**
     * Text as typed: markdown characters escaped, and @ and the mention/link
     * brackets broken with a zero-width space so nothing reads as a mention even
     * where allowed_mentions would not apply.
     */
    static String escape(String text)
    {
        if (text == null)
        {
            return "";
        }
        String s = text.replace("\r", "");
        s = MARKDOWN.matcher(s).replaceAll("\\\\$1");
        s = s.replace("@", "@" + ZERO_WIDTH_SPACE).replace("<", "<" + ZERO_WIDTH_SPACE);
        return s;
    }

    static String truncate(String s, int max)
    {
        if (s.length() <= max)
        {
            return s;
        }
        int end = max - 1;
        // Do not split a surrogate pair or an escape.
        if (Character.isHighSurrogate(s.charAt(end - 1)) || s.charAt(end - 1) == '\\')
        {
            end--;
        }
        return s.substring(0, end) + "…";
    }

    /**
     * Sends off the calling thread. {@code result} gets the HTTP status, or -1 if
     * it never reached Discord; it runs on an OkHttp thread and may be null.
     */
    static void send(OkHttpClient http, HttpUrl url, String payload, IntConsumer result)
    {
        Request request = new Request.Builder()
            .url(url)
            .post(RequestBody.create(JSON, payload))
            .build();
        http.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                // The exception's message can name the URL; its class is enough.
                log.info("Discord webhook alert not sent: {}", e.getClass().getSimpleName());
                if (result != null)
                {
                    result.accept(-1);
                }
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                int code = response.code();
                response.close();
                if (code < 200 || code >= 300)
                {
                    log.info("Discord webhook refused the alert: HTTP {}", code);
                }
                if (result != null)
                {
                    result.accept(code);
                }
            }
        });
    }
}
