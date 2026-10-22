package net.runelite.client.plugins.projectx.questhelper.managers;

import java.lang.reflect.Field;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.projectx.questhelper.helpers.miniquests.alfredgrimhandsbarcrawl.AlfredGrimhandsBarcrawl;
import net.runelite.client.plugins.projectx.questhelper.questhelpers.QuestHelper;
import net.runelite.client.plugins.projectx.questhelper.questinfo.QuestHelperQuest;
import net.runelite.client.plugins.projectx.questhelper.requirements.item.ItemRequirement;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

public class QuestManagerItemRequirementsTest
{
	private AlfredGrimhandsBarcrawl helper;

	@Before
	public void setUp() throws Exception
	{
		helper = new AlfredGrimhandsBarcrawl();
		helper.setQuest(QuestHelperQuest.ALFRED_GRIMHANDS_BARCRAWL);
		Field field = QuestHelper.class.getDeclaredField("configManager");
		field.setAccessible(true);
		field.set(helper, mock(ConfigManager.class));
	}

	@Test
	public void initializesRequirementsBeforeReadingThem()
	{
		SortedMap<QuestHelperQuest, List<ItemRequirement>> reqs = new TreeMap<>();
		SortedMap<QuestHelperQuest, List<ItemRequirement>> recommended = new TreeMap<>();

		QuestManager.collectItemRequirements(List.of(helper), reqs, recommended);

		assertEquals(1, reqs.get(QuestHelperQuest.ALFRED_GRIMHANDS_BARCRAWL).size());
		assertNotNull(recommended.get(QuestHelperQuest.ALFRED_GRIMHANDS_BARCRAWL));
	}
}
