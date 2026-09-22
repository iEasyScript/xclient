package net.runelite.client.plugins.projectx.agentserver.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.client.plugins.projectx.ProjectX;

import java.io.IOException;
import java.util.*;

public class SkillsHandler extends AgentHandler {

	private final Client client;

	public SkillsHandler(Gson gson, Client client) {
		super(gson);
		this.client = client;
	}

	@Override
	public String getPath() {
		return "/skills";
	}

	@Override
	protected void handleRequest(HttpExchange exchange) throws IOException {
		try {
			requireGet(exchange);
		} catch (HttpMethodException e) {
			sendJson(exchange, 405, errorResponse(e.getMessage()));
			return;
		}

		Map<String, String> params = parseQuery(exchange.getRequestURI());
		String nameFilter = params.get("name");

		List<Map<String, Object>> skills = ProjectX.getClientThread().runOnClientThreadOptional(() -> {
			List<Map<String, Object>> result = new ArrayList<>();
			for (Skill skill : Skill.values()) {
				if (skill == Skill.OVERALL) continue;

				if (nameFilter != null && !nameFilter.isEmpty()
						&& !skill.getName().equalsIgnoreCase(nameFilter)) {
					continue;
				}

				Map<String, Object> s = new LinkedHashMap<>();
				s.put("name", skill.getName());
				s.put("level", client.getRealSkillLevel(skill));
				s.put("boostedLevel", client.getBoostedSkillLevel(skill));
				s.put("xp", client.getSkillExperience(skill));
				result.add(s);
			}
			return result;
		}).orElse(Collections.emptyList());

		int totalLevel = skills.stream()
				.mapToInt(s -> (int) s.get("level"))
				.sum();

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("count", skills.size());
		response.put("totalLevel", totalLevel);
		response.put("skills", skills);
		sendJson(exchange, 200, response);
	}
}
