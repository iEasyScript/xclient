package net.runelite.client.plugins.projectx.questhelper.logic;

import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.questhelper.QuestHelperPlugin;

public abstract class BaseQuest implements IQuest {

    protected QuestHelperPlugin getQuestHelperPlugin() {
        return (QuestHelperPlugin) ProjectX.getPluginManager().getPlugins().stream().filter(x -> x instanceof QuestHelperPlugin).findFirst().orElse(null);
    }
}
