package net.runelite.client.plugins.projectx.bot;

import net.runelite.client.plugins.projectx.util.dialogues.Rs2Dialogue;

/**
 * NPC chat and option dialogues.
 *
 * <pre>{@code
 * if (Dialogue.isInDialogue()) {
 *     Dialogue.clickContinue();
 * }
 * }</pre>
 *
 * <p>Every static method and field of {@link Rs2Dialogue} is available here under the shorter name,
 * so {@code Rs2Dialogue.anything(...)} can be written {@code Dialogue.anything(...)}.
 */
@SuppressWarnings({"deprecation", "removal"})
public final class Dialogue extends Rs2Dialogue
{
    private Dialogue()
    {
    }
}
