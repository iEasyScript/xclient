package net.runelite.client.plugins.projectx.alerts;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import java.util.Arrays;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DiscordWebhookTest
{
    private static final Gson GSON = new Gson();
    private static final String TOKEN = "AbCdEfGhIjKlMnOpQrStUvWxYz0123456789_-AbCdEfGhIjKlMnOpQrStUvWxYz0123";
    private static final String GOOD = "https://discord.com/api/webhooks/123456789012345678/" + TOKEN;

    // ---- URL validation

    @Test
    public void acceptsDiscordWebhooks()
    {
        assertTrue(DiscordWebhook.isValid(GOOD));
        assertTrue(DiscordWebhook.isValid("  " + GOOD + "  "));
        assertTrue(DiscordWebhook.isValid("https://discordapp.com/api/webhooks/123456789012345678/" + TOKEN));
        assertTrue(DiscordWebhook.isValid("https://ptb.discord.com/api/webhooks/123456789012345678/" + TOKEN));
        assertTrue(DiscordWebhook.isValid("https://canary.discord.com/api/webhooks/123456789012345678/" + TOKEN));
        assertTrue(DiscordWebhook.isValid(GOOD + "?thread_id=987654321098765432"));
        assertTrue(DiscordWebhook.isValid("HTTPS://Discord.com/api/webhooks/123456789012345678/" + TOKEN));
    }

    @Test
    public void refusesAnythingElse()
    {
        assertFalse(DiscordWebhook.isValid(null));
        assertFalse(DiscordWebhook.isValid(""));
        assertFalse(DiscordWebhook.isValid("   "));
        assertFalse(DiscordWebhook.isValid("not a url"));
        // Plain http would hand the credential to anyone on the path.
        assertFalse(DiscordWebhook.isValid("http://discord.com/api/webhooks/123456789012345678/" + TOKEN));
        // No scheme: not guessed.
        assertFalse(DiscordWebhook.isValid("discord.com/api/webhooks/123456789012345678/" + TOKEN));
        // Look-alike and other hosts.
        assertFalse(DiscordWebhook.isValid("https://discord.com.evil.example/api/webhooks/123456789012345678/" + TOKEN));
        assertFalse(DiscordWebhook.isValid("https://evil.example/api/webhooks/123456789012345678/" + TOKEN));
        assertFalse(DiscordWebhook.isValid("https://evildiscord.com/api/webhooks/123456789012345678/" + TOKEN));
        assertFalse(DiscordWebhook.isValid("https://discord.gg/api/webhooks/123456789012345678/" + TOKEN));
        // Credentials and a host smuggled through userinfo.
        assertFalse(DiscordWebhook.isValid("https://discord.com@evil.example/api/webhooks/123456789012345678/" + TOKEN));
        assertFalse(DiscordWebhook.isValid("https://user:pass@discord.com/api/webhooks/123456789012345678/" + TOKEN));
        // Another port.
        assertFalse(DiscordWebhook.isValid("https://discord.com:8443/api/webhooks/123456789012345678/" + TOKEN));
        // Not a webhook path.
        assertFalse(DiscordWebhook.isValid("https://discord.com/channels/123456789012345678/123456789012345678"));
        assertFalse(DiscordWebhook.isValid("https://discord.com/api/webhooks/123456789012345678"));
        assertFalse(DiscordWebhook.isValid("https://discord.com/api/webhooks/notanid/" + TOKEN));
        assertFalse(DiscordWebhook.isValid("https://discord.com/api/webhooks/123456789012345678/" + TOKEN + "/slack"));
        assertFalse(DiscordWebhook.isValid("https://discord.com/api/webhooks/123456789012345678/short"));
        // Other query parameters or a fragment.
        assertFalse(DiscordWebhook.isValid(GOOD + "?wait=true&x=1"));
        assertFalse(DiscordWebhook.isValid(GOOD + "?thread_id=abc"));
        assertFalse(DiscordWebhook.isValid(GOOD + "#frag"));
    }

    @Test
    public void parseKeepsTheUrl()
    {
        assertNotNull(DiscordWebhook.parse(GOOD));
        assertEquals("discord.com", DiscordWebhook.parse(GOOD).host());
        assertNull(DiscordWebhook.parse("https://example.com/"));
    }

    // ---- payload

    @Test
    public void payloadMentionsNoOne()
    {
        Alert alert = new Alert(AlertType.DEATH, "@everyone you died", "<@123> and @here and <@&456>",
            Arrays.asList(new Alert.Field("Script", "@everyone", true)), "Zezima", 0L);
        JsonObject json = GSON.fromJson(DiscordWebhook.buildPayload(alert), JsonObject.class);

        JsonArray parse = json.getAsJsonObject("allowed_mentions").getAsJsonArray("parse");
        assertEquals("allowed_mentions.parse must be empty", 0, parse.size());
        assertFalse("no plain content to ping from", json.has("content"));

        String everything = json.toString();
        JsonObject embed = json.getAsJsonArray("embeds").get(0).getAsJsonObject();
        String title = embed.get("title").getAsString();
        String description = embed.get("description").getAsString();
        assertFalse(title.contains("@everyone"));
        assertFalse(description.contains("@here"));
        assertFalse(description.contains("<@"));
        assertFalse(everything, embed.getAsJsonArray("fields").get(1).getAsJsonObject().get("value").getAsString().contains("@everyone"));
    }

    @Test
    public void payloadEscapesJsonAndMarkdown()
    {
        String nasty = "quote \" backslash \\ newline\nTab\t*bold* _it_ `code` ~~s~~ |sp| > q # h [l](x) é 😀";
        Alert alert = new Alert(AlertType.SCRIPT_STOPPED, "Script stopped: " + nasty, nasty, null, "Name_With_Underscores", 0L);
        String payload = DiscordWebhook.buildPayload(alert);

        // Valid JSON whatever the text held.
        JsonObject json = GSON.fromJson(payload, JsonObject.class);
        JsonObject embed = json.getAsJsonArray("embeds").get(0).getAsJsonObject();
        String description = embed.get("description").getAsString();

        assertTrue(description.contains("quote \" backslash \\\\ newline\nTab"));
        assertTrue(description.contains("\\*bold\\*"));
        assertTrue(description.contains("\\_it\\_"));
        assertTrue(description.contains("\\`code\\`"));
        assertTrue(description.contains("\\~\\~s\\~\\~"));
        assertTrue(description.contains("\\|sp\\|"));
        assertTrue(description.contains("\\> q \\# h \\[l\\](x)"));
        assertTrue(description.contains("é 😀"));

        JsonObject player = embed.getAsJsonArray("fields").get(0).getAsJsonObject();
        assertEquals("Player", player.get("name").getAsString());
        assertEquals("Name\\_With\\_Underscores", player.get("value").getAsString());
        assertEquals(AlertType.SCRIPT_STOPPED.getColor(), embed.get("color").getAsInt());
        assertEquals("1970-01-01T00:00:00Z", embed.get("timestamp").getAsString());
    }

    @Test
    public void payloadLeavesOutPlayerWhenNotAllowed()
    {
        Alert alert = new Alert(AlertType.LEVEL_UP, "Mining level 61", "", null, null, 0L);
        JsonObject embed = GSON.fromJson(DiscordWebhook.buildPayload(alert), JsonObject.class)
            .getAsJsonArray("embeds").get(0).getAsJsonObject();
        assertFalse(embed.has("fields"));
        assertFalse(embed.has("description"));
        assertFalse(embed.toString().contains("Player"));
    }

    @Test
    public void payloadKeepsToDiscordLimits()
    {
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 6000; i++)
        {
            longText.append('a');
        }
        Alert.Field[] many = new Alert.Field[40];
        for (int i = 0; i < many.length; i++)
        {
            many[i] = new Alert.Field("f" + i, longText.toString(), false);
        }
        Alert alert = new Alert(AlertType.TEST, longText.toString(), longText.toString(), Arrays.asList(many), "p", 0L);
        JsonObject embed = GSON.fromJson(DiscordWebhook.buildPayload(alert), JsonObject.class)
            .getAsJsonArray("embeds").get(0).getAsJsonObject();
        assertTrue(embed.get("title").getAsString().length() <= 256);
        assertTrue(embed.get("description").getAsString().length() <= 4096);
        assertEquals(25, embed.getAsJsonArray("fields").size());
        assertTrue(embed.getAsJsonArray("fields").get(3).getAsJsonObject().get("value").getAsString().length() <= 1024);
    }

    @Test
    public void escapeHandlesNull()
    {
        assertEquals("", DiscordWebhook.escape(null));
        assertEquals("plain text 123", DiscordWebhook.escape("plain text 123"));
    }
}
