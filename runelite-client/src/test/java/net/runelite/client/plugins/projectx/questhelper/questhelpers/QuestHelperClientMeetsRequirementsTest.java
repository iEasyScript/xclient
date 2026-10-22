package net.runelite.client.plugins.projectx.questhelper.questhelpers;

import java.lang.reflect.Field;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.projectx.questhelper.helpers.quests.currentaffairs.CurrentAffairs;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QuestHelperClientMeetsRequirementsTest
{
	private CurrentAffairs helper;

	@Before
	public void setUp() throws Exception
	{
		helper = new CurrentAffairs();
		Client client = mock(Client.class);
		when(client.getIntStack()).thenReturn(new int[]{1});
		inject("client", client);
		inject("configManager", mock(ConfigManager.class));
	}

	private void inject(String name, Object value) throws Exception
	{
		Field field = QuestHelper.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(helper, value);
	}

	@Test
	public void initializesRequirementsBeforeReadingThem()
	{
		assertFalse(helper.clientMeetsRequirements());
	}
}
