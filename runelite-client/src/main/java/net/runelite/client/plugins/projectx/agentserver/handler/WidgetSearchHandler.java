package net.runelite.client.plugins.projectx.agentserver.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import net.runelite.client.plugins.projectx.util.widget.Rs2WidgetInspector;
import net.runelite.client.plugins.projectx.util.widget.Rs2WidgetInspector.WidgetDescription;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WidgetSearchHandler extends AgentHandler {

	private final int defaultLimit;

	public WidgetSearchHandler(Gson gson, int defaultLimit) {
		super(gson);
		this.defaultLimit = defaultLimit;
	}

	@Override
	public String getPath() {
		return "/widgets/search";
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
		String q = params.get("q");
		if (q == null || q.isEmpty()) {
			sendJson(exchange, 400, errorResponse("Missing required parameter: q"));
			return;
		}

		int limit = getIntParam(params, "limit", defaultLimit);
		String[] keywords = q.split("[,\\s]+");
		List<WidgetDescription> results = Rs2WidgetInspector.search(keywords);

		if (results.size() > limit) {
			results = results.subList(0, limit);
		}

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("query", q);
		response.put("count", results.size());
		response.put("results", results);
		sendJson(exchange, 200, response);
	}
}
