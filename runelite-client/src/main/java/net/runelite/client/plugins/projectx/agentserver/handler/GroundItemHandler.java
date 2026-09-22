package net.runelite.client.plugins.projectx.agentserver.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.api.tileitem.models.Rs2TileItemModel;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class GroundItemHandler extends AgentHandler {

	private final int defaultLimit;

	public GroundItemHandler(Gson gson, int defaultLimit) {
		super(gson);
		this.defaultLimit = defaultLimit;
	}

	@Override
	public String getPath() {
		return "/ground-items";
	}

	@Override
	protected void handleRequest(HttpExchange exchange) throws IOException {
		String sub = getSubPath(exchange, "/ground-items");

		switch (sub) {
			case "":
			case "/":
				handleQuery(exchange);
				break;
			case "/pickup":
				handlePickup(exchange);
				break;
			default:
				sendJson(exchange, 404, errorResponse("Unknown path: /ground-items" + sub));
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

		var query = ProjectX.getRs2TileItemCache().query();
		if (id >= 0) {
			query = query.withId(id);
		}
		if (name != null && !name.isEmpty()) {
			query = query.withName(name);
		} else if (nameContains != null && !nameContains.isEmpty()) {
			query = query.withNameContains(nameContains);
		}
		query = query.within(maxDistance);

		List<Rs2TileItemModel> items = query.toListOnClientThread();

		List<Map<String, Object>> serialized = items.stream()
				.limit(limit)
				.map(this::serializeItem)
				.collect(Collectors.toList());

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("count", serialized.size());
		response.put("total", items.size());
		response.put("items", serialized);
		sendJson(exchange, 200, response);
	}

	private void handlePickup(HttpExchange exchange) throws IOException {
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

		String name = (String) body.get("name");
		Number idNum = (Number) body.get("id");

		Rs2TileItemModel item = null;
		if (name != null && !name.isEmpty()) {
			item = ProjectX.getRs2TileItemCache().query()
					.withName(name)
					.nearestOnClientThread();
		} else if (idNum != null) {
			item = ProjectX.getRs2TileItemCache().query()
					.withId(idNum.intValue())
					.nearestOnClientThread();
		} else {
			sendJson(exchange, 400, errorResponse("Provide either name or id"));
			return;
		}

		Map<String, Object> response = new LinkedHashMap<>();
		if (item == null) {
			response.put("success", false);
			response.put("reason", "Ground item not found");
		} else {
			boolean picked = item.pickup();
			response.put("success", picked);
			response.put("item", serializeItem(item));
		}
		sendJson(exchange, 200, response);
	}

	private Map<String, Object> serializeItem(Rs2TileItemModel item) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("id", item.getId());
		map.put("name", item.getName());
		map.put("quantity", item.getQuantity());
		map.put("geValue", item.getTotalGeValue());
		map.put("lootable", item.isLootAble());

		WorldPoint loc = item.getWorldLocation();
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
