package net.runelite.client.plugins.projectx.util.discord;

import net.runelite.client.plugins.projectx.util.discord.models.DiscordEmbed;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Rs2DiscordPlayerFieldTest {

	@Test
	public void nullPlayerNameFallsBackToUnknown() {
		DiscordEmbed.Field field = Rs2Discord.createPlayerField(null);
		assertEquals("Player", field.getName());
		assertEquals("Unknown", field.getValue());
	}

	@Test
	public void emptyPlayerNameFallsBackToUnknown() {
		assertEquals("Unknown", Rs2Discord.createPlayerField("").getValue());
	}

	@Test
	public void playerNameIsUsedWhenPresent() {
		assertEquals("Zezima", Rs2Discord.createPlayerField("Zezima").getValue());
	}
}
