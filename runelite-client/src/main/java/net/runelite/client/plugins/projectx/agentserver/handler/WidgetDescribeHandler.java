package net.runelite.client.plugins.projectx.agentserver.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import net.runelite.client.plugins.projectx.util.widget.Rs2WidgetInspector;
import net.runelite.client.plugins.projectx.util.widget.Rs2WidgetInspector.WidgetDescription;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WidgetDescribeHandler extends AgentHandler {

	private static final int MAX_DEPTH = 15;

	public WidgetDescribeHandler(Gson gson) {
		super(gson);
	}

	@Override
	public String getPath() {
		return "/widgets/describe";
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
		String groupStr = params.get("groupId");
		String childStr = params.get("childId");

		if (groupStr == null || childStr == null) {
			sendJson(exchange, 400, errorResponse("Missing required parameters: groupId, childId"));
			return;
		}

		int groupId, childId;
		try {
			groupId = Integer.parseInt(groupStr);
			childId = Integer.parseInt(childStr);
		} catch (NumberFormatException e) {
			sendJson(exchange, 400, errorResponse("groupId and childId must be integers"));
			return;
		}

		int depth = Math.min(getIntParam(params, "depth", 5), MAX_DEPTH);

		List<WidgetDescription> tree = Rs2WidgetInspector.describeWidget(groupId, childId, depth);

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("groupId", groupId);
		response.put("childId", childId);
		response.put("depth", depth);
		response.put("count", tree.size());
		response.put("widgets", tree);
		sendJson(exchange, 200, response);
	}
}
