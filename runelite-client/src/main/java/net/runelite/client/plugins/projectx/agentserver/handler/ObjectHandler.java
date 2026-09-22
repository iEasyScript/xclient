package net.runelite.client.plugins.projectx.agentserver.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.api.tileobject.models.Rs2TileObjectModel;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ObjectHandler extends AgentHandler {

	private final int defaultLimit;

	public ObjectHandler(Gson gson, int defaultLimit) {
		super(gson);
		this.defaultLimit = defaultLimit;
	}

	@Override
	public String getPath() {
		return "/objects";
	}

	@Override
	protected void handleRequest(HttpExchange exchange) throws IOException {
		String sub = getSubPath(exchange, "/objects");

		switch (sub) {
			case "":
			case "/":
				handleQuery(exchange);
				break;
			case "/interact":
				handleInteract(exchange);
				break;
			default:
				sendJson(exchange, 404, errorResponse("Unknown path: /objects" + sub));
		}
	}

	private void handleQuery(HttpExchange exchange) throws IOException {
		try {
			requireGet(exchange);
		} catch (HttpMethodException e) {
			sendJson(exchange, 405, errorResponse(e.getMessage()));
			return;
		}

		Map<String, String> params = parseQuery(exchange.getRequestURI());
		String name = params.get("name");
		String nameContains = params.get("nameContains");
		int id = getIntParam(params, "id", -1);
		int maxDistance = getIntParam(params, "maxDistance", 20);
		int limit = getIntParam(params, "limit", defaultLimit);

		var query = ProjectX.getRs2TileObjectCache().query();
		if (id >= 0) {
			query = query.withId(id);
		}
		if (name != null && !name.isEmpty()) {
			query = query.withName(name);
		} else if (nameContains != null && !nameContains.isEmpty()) {
			query = query.withNameContains(nameContains);
		}
		query = query.within(maxDistance);

		List<Rs2TileObjectModel> objects = query.toListOnClientThread();

		List<Map<String, Object>> serialized = objects.stream()
				.limit(limit)
				.map(this::serializeObject)
				.collect(Collectors.toList());

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("count", serialized.size());
		response.put("total", objects.size());
		response.put("objects", serialized);
		sendJson(exchange, 200, response);
	}

	private void handleInteract(HttpExchange exchange) throws IOException {
		try {
			requirePost(exchange);
		} catch (HttpMethodException e) {
			sendJson(exchange, 405, errorResponse(e.getMessage()));
			return;
		}

		Map<String, Object> body;
		try {
			body = readJsonBody(exchange);
		} catch (Exception e) {
			sendJson(exchange, 400, errorResponse("Invalid JSON body"));
			return;
		}

		String action = (String) body.get("action");
		if (action == null || action.isEmpty()) {
			sendJson(exchange, 400, errorResponse("Missing required field: action"));
			return;
		}

		String name = (String) body.get("name");
		Number idNum = (Number) body.get("id");

		Rs2TileObjectModel obj = null;
		if (name != null && !name.isEmpty()) {
			obj = ProjectX.getRs2TileObjectCache().query()
					.withName(name)
					.nearestOnClientThread();
		} else if (idNum != null) {
			obj = ProjectX.getRs2TileObjectCache().query()
					.withId(idNum.intValue())
					.nearestOnClientThread();
		} else {
			sendJson(exchange, 400, errorResponse("Provide either name or id"));
			return;
		}

		Map<String, Object> response = new LinkedHashMap<>();
		if (obj == null) {
			response.put("success", false);
			response.put("reason", "Object not found");
		} else {
			boolean clicked = obj.click(action);
			response.put("success", clicked);
			response.put("object", serializeObject(obj));
			response.put("action", action);
		}
		sendJson(exchange, 200, response);
	}

	private Map<String, Object> serializeObject(Rs2TileObjectModel obj) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("id", obj.getId());
		map.put("name", obj.getName());
		map.put("type", obj.getTileObjectType().name());
		map.put("reachable", obj.isReachable());

		WorldPoint loc = obj.getWorldLocation();
		if (loc != null) {
			Map<String, Integer> position = new LinkedHashMap<>();
			position.put("x", loc.getX());
			position.put("y", loc.getY());
			position.put("plane", loc.getPlane());
			map.put("position", position);
		}
		return map;
	}
}
