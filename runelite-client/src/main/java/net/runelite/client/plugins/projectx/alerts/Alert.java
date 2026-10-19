package net.runelite.client.plugins.projectx.alerts;

import java.util.Collections;
import java.util.List;
import lombok.Value;

/**
 * One alert, ready for any channel. Plain text only: each channel does its own
 * escaping. Never carries the account email, tokens or passwords; {@code player}
 * is the in-game name, and only when the player allowed it.
 */
@Value
public class Alert
{
    AlertType type;
    String title;
    String description;
    List<Field> fields;
    /** In-game name, or null when it is not to be shown. */
    String player;
    long at;

    public Alert(AlertType type, String title, String description, List<Field> fields, String player, long at)
    {
        this.type = type;
        this.title = title == null ? "" : title;
        this.description = description == null ? "" : description;
        this.fields = fields == null ? Collections.emptyList() : Collections.unmodifiableList(fields);
        this.player = player;
        this.at = at;
    }

    @Value
    public static class Field
    {
        String name;
        String value;
        boolean inline;
    }

    /** One line for the desktop notification. */
    public String toNotificationText()
    {
        String text = description.isEmpty() ? title : title + ": " + description;
        return text.replaceAll("\\s*\\R\\s*", "; ");
    }
}
