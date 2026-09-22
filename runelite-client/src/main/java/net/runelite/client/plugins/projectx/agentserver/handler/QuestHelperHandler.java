package net.runelite.client.plugins.projectx.agentserver.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.QuestState;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.questhelper.QuestHelperConfig;
import net.runelite.client.plugins.projectx.questhelper.QuestHelperPlugin;
import net.runelite.client.plugins.projectx.questhelper.questhelpers.QuestHelper;
import net.runelite.client.plugins.projectx.questhelper.questinfo.QuestHelperQuest;
import net.runelite.client.plugins.projectx.questhelper.steps.QuestStep;

public class QuestHelperHandler extends AgentHandler
{
	private static final String BASE_PATH = "/quest-helper";

	public QuestHelperHandler(Gson gson)
	{
		super(gson);
	}

	@Override
	public String getPath()
	{
		return BASE_PATH;
	}

	@Override
	protected void handleRequest(HttpExchange exchange) throws IOException
	{
		String sub = getSubPath(exchange, BASE_PATH);

		switch (sub)
		{
			case "/status":
				handleStatus(exchange);
				break;
			case "/start":
				handleStart(exchange);
				break;
			case "/stop":
				handleStop(exchange);
				break;
			case "/list":
			case "":
			case "/":
				handleList(exchange);
				break;
			case "/states":
				handleStates(exchange);
				break;
			default:
				sendJson(exchange, 404, errorResponse("Unknown endpoint: " + BASE_PATH + sub));
		}
	}

	private void handleStatus(HttpExchange exchange) throws IOException
	{
		try
		{
			requireGet(exchange);
		}
		catch (HttpMethodException e)
		{
			sendJson(exchange, 405, errorResponse(e.getMessage()));
			return;
		}

		QuestHelperPlugin plugin = ProjectX.getPlugin(QuestHelperPlugin.class);
		if (plugin == null)
		{
			sendJson(exchange, 404, errorResponse("Quest Helper plugin not found"));
			return;
		}

		Map<String, Object> result = new LinkedHashMap<>();
		PluginManager pluginManager = ProjectX.getPluginManager();
		result.put("active", pluginManager != null && pluginManager.isActive(plugin));
		result.put("enabled", pluginManager != null && pluginManager.isPluginEnabled(plugin));
		result.put("turnOn", ProjectX.getConfigManager()
			.getConfiguration(QuestHelperConfig.QUEST_HELPER_GROUP, "TurnOn"));

		QuestHelper selectedQuest = plugin.getSelectedQuest();
		result.put("selected", selectedQuest != null);
		if (selectedQuest != null)
		{
			result.put("questName", selectedQuest.getQuest().getName());
			result.put("completed", ProjectX.getClientThread()
				.runOnClientThreadOptional(selectedQuest::isCompleted)
				.orElse(false));

			QuestStep currentStep = selectedQuest.getCurrentStep();
			if (currentStep != null)
			{
				QuestStep activeStep = currentStep.getActiveStep();
				result.put("stepText", activeStep != null ? activeStep.getText() : currentStep.getText());
				result.put("stepClass", activeStep != null
					? activeStep.getClass().getSimpleName()
					: currentStep.getClass().getSimpleName());
			}
		}

		sendJson(exchange, 200, result);
	}

	private void handleStart(HttpExchange exchange) throws IOException
	{
		try
		{
			requirePost(exchange);
		}
		catch (HttpMethodException e)
		{
			sendJson(exchange, 405, errorResponse(e.getMessage()));
			return;
		}

		Map<String, Object> body = readJsonBody(exchange);
		String name = (String) body.get("name");
		if (name == null || name.isEmpty())
		{
			sendJson(exchange, 400, errorResponse("name is required"));
			return;
		}

		QuestHelperPlugin plugin = ProjectX.getPlugin(QuestHelperPlugin.class);
		if (plugin == null)
		{
			sendJson(exchange, 404, errorResponse("Quest Helper plugin not found"));
			return;
		}

		if (!plugin.startQuestHelper(name))
		{
			sendJson(exchange, 404, errorResponse("Quest helper not found: " + name));
			return;
		}

		ProjectX.getConfigManager().setRSProfileConfiguration(
			QuestHelperConfig.QUEST_HELPER_GROUP,
			"TurnOn",
			true);

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("success", true);
		result.put("questName", name);
		sendJson(exchange, 200, result);
	}

	private void handleStop(HttpExchange exchange) throws IOException
	{
		try
		{
			requirePost(exchange);
		}
		catch (HttpMethodException e)
		{
			sendJson(exchange, 405, errorResponse(e.getMessage()));
			return;
		}

		QuestHelperPlugin plugin = ProjectX.getPlugin(QuestHelperPlugin.class);
		if (plugin == null)
		{
			sendJson(exchange, 404, errorResponse("Quest Helper plugin not found"));
			return;
		}

		plugin.getQuestManager().shutDownQuest(true);

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("success", true);
		sendJson(exchange, 200, result);
	}

	private void handleList(HttpExchange exchange) throws IOException
	{
		try
		{
			requireGet(exchange);
		}
		catch (HttpMethodException e)
		{
			sendJson(exchange, 405, errorResponse(e.getMessage()));
			return;
		}

		List<Map<String, Object>> quests = new ArrayList<>();
		for (QuestHelperQuest qhq : QuestHelperQuest.values())
		{
			Map<String, Object> entry = new LinkedHashMap<>();
			entry.put("enum", qhq.name());
			entry.put("displayName", qhq.getName());
			quests.add(entry);
		}

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("count", quests.size());
		result.put("quests", quests);
		sendJson(exchange, 200, result);
	}

	private void handleStates(HttpExchange exchange) throws IOException
	{
		try
		{
			requireGet(exchange);
		}
		catch (HttpMethodException e)
		{
			sendJson(exchange, 405, errorResponse(e.getMessage()));
			return;
		}

		Client client = ProjectX.getClient();
		if (client == null)
		{
			sendJson(exchange, 503, errorResponse("Client not available"));
			return;
		}

		List<Map<String, Object>> states = ProjectX.getClientThread()
			.runOnClientThreadOptional(() ->
			{
				List<Map<String, Object>> out = new ArrayList<>();
				for (QuestHelperQuest qhq : QuestHelperQuest.values())
				{
					Map<String, Object> entry = new LinkedHashMap<>();
					entry.put("enum", qhq.name());
					entry.put("displayName", qhq.getName());
					try
					{
						QuestState state = qhq.getState(client);
						entry.put("state", state != null ? state.name() : "UNKNOWN");
					}
					catch (Exception ex)
					{
						entry.put("state", "ERROR");
						entry.put("error", ex.getClass().getSimpleName() + ": " + ex.getMessage());
					}
					out.add(entry);
				}
				return out;
			})
			.orElse(new ArrayList<>());

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("count", states.size());
		result.put("quests", states);
		sendJson(exchange, 200, result);
	}
}
