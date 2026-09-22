package net.runelite.client.plugins.projectx.agentserver.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import net.runelite.client.plugins.projectx.util.widget.Rs2WidgetInspector;
import net.runelite.client.plugins.projectx.util.widget.Rs2WidgetInspector.WidgetDescription;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class WidgetListHandler extends AgentHandler {

	private final int defaultLimit;

	public WidgetListHandler(Gson gson, int defaultLimit) {
		super(gson);
		this.defaultLimit = defaultLimit;
	}

	@Override
	public String getPath() {
		return "/widgets/list";
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
		int limit = getIntParam(params, "limit", defaultLimit);
		int offset = getIntParam(params, "offset", 0);

		List<WidgetDescription> widgets = Rs2WidgetInspector.getVisibleInterfaces();
		sendJson(exchange, 200, paginate(widgets, offset, limit, "widgets"));
	}
}
