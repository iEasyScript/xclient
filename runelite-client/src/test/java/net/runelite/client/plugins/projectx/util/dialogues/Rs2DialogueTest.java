package net.runelite.client.plugins.projectx.util.dialogues;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Rs2DialogueTest {
    @Test
    public void isContinuePromptTextAcceptsContinuePrompt() {
        assertTrue(Rs2Dialogue.isContinuePromptText("<col=ffffff>Click here to continue</col>"));
    }

    @Test
    public void isContinuePromptTextRejectsChatboxInputs() {
        assertFalse(Rs2Dialogue.isContinuePromptText("Enter amount:"));
        assertFalse(Rs2Dialogue.isContinuePromptText("Search"));
        assertFalse(Rs2Dialogue.isContinuePromptText("abyssal whip"));
    }
}
