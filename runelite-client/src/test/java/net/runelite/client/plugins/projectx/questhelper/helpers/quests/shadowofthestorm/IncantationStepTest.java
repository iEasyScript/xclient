package net.runelite.client.plugins.projectx.questhelper.helpers.quests.shadowofthestorm;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.runelite.api.Client;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.projectx.questhelper.questhelpers.QuestHelper;
import net.runelite.client.plugins.projectx.questhelper.steps.QuestStep;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IncantationStepTest
{
	private Client client;
	private IncantationStep step;

	@Before
	public void setUp() throws Exception
	{
		client = mock(Client.class);
		step = new IncantationStep(mock(QuestHelper.class), false);
		Field clientField = QuestStep.class.getDeclaredField("client");
		clientField.setAccessible(true);
		clientField.set(step, client);
	}

	private void givenIncantationVarbits()
	{
		when(client.getVarbitValue(VarbitID.AGRITH_INCANTATION_1)).thenReturn(0);
		when(client.getVarbitValue(VarbitID.AGRITH_INCANTATION_2)).thenReturn(1);
		when(client.getVarbitValue(VarbitID.AGRITH_INCANTATION_3)).thenReturn(2);
		when(client.getVarbitValue(VarbitID.AGRITH_INCANTATION_4)).thenReturn(3);
		when(client.getVarbitValue(VarbitID.AGRITH_INCANTATION_5)).thenReturn(4);
	}

	private void givenIncantationChatMenu()
	{
		Widget option = mock(Widget.class);
		when(option.getText()).thenReturn("Nahudu");
		Widget menu = mock(Widget.class);
		when(menu.getChildren()).thenReturn(new Widget[]{mock(Widget.class), mock(Widget.class), option});
		when(menu.getChild(2)).thenReturn(option);
		when(client.getWidget(InterfaceID.CHATMENU, 1)).thenReturn(menu);
	}

	private void updateChoice() throws Exception
	{
		Method method = IncantationStep.class.getDeclaredMethod("updateChoiceIfRequired");
		method.setAccessible(true);
		method.invoke(step);
	}

	private int position() throws Exception
	{
		Field field = IncantationStep.class.getDeclaredField("incantationPosition");
		field.setAccessible(true);
		return field.getInt(step);
	}

	@Test
	public void chantOnInventorySigilResetsPosition() throws Exception
	{
		givenIncantationVarbits();
		givenIncantationChatMenu();
		step.updateHints();
		updateChoice();
		updateChoice();

		Widget sigil = mock(Widget.class);
		when(sigil.getId()).thenReturn(InterfaceID.Inventory.ITEMS);
		MenuEntry entry = mock(MenuEntry.class);
		when(entry.getWidget()).thenReturn(sigil);
		when(entry.getOption()).thenReturn("Chant");
		step.onMenuOptionClicked(new MenuOptionClicked(entry));

		assertEquals(0, position());
	}

	@Test
	public void repeatedChantMenusDoNotRunPastIncantation() throws Exception
	{
		givenIncantationVarbits();
		givenIncantationChatMenu();
		step.updateHints();

		for (int i = 0; i < 6; i++)
		{
			updateChoice();
		}

		assertEquals(1, position());
	}

	@Test
	public void chantMenuBeforeOrderIsKnownIsIgnored() throws Exception
	{
		givenIncantationChatMenu();
		step.updateHints();

		updateChoice();

		assertEquals(0, position());
	}
}
