package net.runelite.client.plugins.skillcalculator.skills;

import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.projectx.util.widget.Rs2Widget;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

public class MagicActionTest
{
	@Test
	public void npcContactUsesTheGameNameAndStillResolvesTheOldOne()
	{
		assertEquals("Astral Contact", MagicAction.NPC_CONTACT.getName());
		assertEquals(MagicAction.NPC_CONTACT, MagicAction.fromString("Astral Contact"));
		assertEquals(MagicAction.NPC_CONTACT, MagicAction.fromString("npc contact"));
		assertEquals(MagicAction.NPC_CONTACT, MagicAction.fromString("Npc Contact"));
	}

	@Test
	public void formerNameDoesNotChangeWhichSpellAPartialNameFindsFirst()
	{
		assertEquals(MagicAction.CONFUSE, MagicAction.fromString("c"));
		assertEquals(MagicAction.HIGH_LEVEL_ALCHEMY, MagicAction.fromString("High Level Alchemy"));
		assertNull(MagicAction.fromString(null));
	}

	@Test
	public void npcContactIsFoundByItsSpellbookComponent()
	{
		Widget spell = mock(Widget.class);
		when(spell.getId()).thenReturn(InterfaceID.MagicSpellbook.NPC_CONTACT);
		when(spell.getActions()).thenReturn(new String[]{"Cast"});
		try (MockedStatic<Rs2Widget> widgets = Mockito.mockStatic(Rs2Widget.class))
		{
			widgets.when(() -> Rs2Widget.getWidget(InterfaceID.MagicSpellbook.NPC_CONTACT)).thenReturn(spell);

			assertEquals(InterfaceID.MagicSpellbook.NPC_CONTACT, MagicAction.NPC_CONTACT.getWidgetId());
			assertArrayEquals(new String[]{"Cast"}, MagicAction.NPC_CONTACT.getActions());

			widgets.verify(() -> Rs2Widget.findWidget(anyString(), anyList()), never());
		}
	}

	@Test
	public void missingSpellGivesNoWidgetInsteadOfThrowing()
	{
		try (MockedStatic<Rs2Widget> widgets = Mockito.mockStatic(Rs2Widget.class))
		{
			// Nothing loaded: the component, the spellbook root and the name lookup all come back empty.
			widgets.when(() -> Rs2Widget.getWidget(anyInt())).thenReturn(null);
			widgets.when(() -> Rs2Widget.getWidget(anyInt(), anyInt())).thenReturn(null);

			assertEquals(-1, MagicAction.NPC_CONTACT.getWidgetId());
			assertNull(MagicAction.NPC_CONTACT.getActions());
			assertEquals(-1, MagicAction.HIGH_LEVEL_ALCHEMY.getWidgetId());
			assertNull(MagicAction.HIGH_LEVEL_ALCHEMY.getActions());

			Widget spellbook = mock(Widget.class);
			when(spellbook.getStaticChildren()).thenReturn(new Widget[]{null});
			widgets.when(() -> Rs2Widget.getWidget(218, 0)).thenReturn(spellbook);
			widgets.when(() -> Rs2Widget.findWidget(anyString(), anyList())).thenReturn(null);

			assertEquals(-1, MagicAction.HIGH_LEVEL_ALCHEMY.getWidgetId());
			assertNull(MagicAction.HIGH_LEVEL_ALCHEMY.getActions());
		}
	}
}
