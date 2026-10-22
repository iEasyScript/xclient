package net.runelite.client.plugins.projectx.questhelper.panel;

import java.util.Collections;
import net.runelite.client.plugins.projectx.questhelper.QuestHelperPlugin;
import net.runelite.client.plugins.projectx.questhelper.managers.QuestManager;
import net.runelite.client.plugins.projectx.questhelper.questhelpers.QuestHelper;
import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class QuestOverviewPanelTest
{
	@Test
	public void addQuestToleratesCurrentStepClearedBeforeEdtRuns()
	{
		QuestOverviewPanel panel = new QuestOverviewPanel(mock(QuestHelperPlugin.class), mock(QuestManager.class));
		QuestHelper quest = mock(QuestHelper.class);
		when(quest.getPanels()).thenReturn(Collections.emptyList());
		when(quest.getCurrentStep()).thenReturn(null);

		panel.addQuest(quest, true);

		assertSame(quest, panel.currentQuest);
		verify(quest, never()).getQuest();
	}
}
