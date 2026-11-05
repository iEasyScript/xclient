package net.runelite.client.plugins.projectx.agentserver.mcp;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import net.runelite.client.plugins.projectx.agentserver.handler.AgentHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * {@code /mcp}: the MCP Streamable HTTP endpoint. POST carries JSON-RPC and is answered with
 * {@code application/json}; GET is 405 (no server-initiated stream); DELETE ends a session.
 * The checks in {@link AgentHandler#handle} run first: loopback Host, no Origin, and the token in
 * {@code Authorization: Bearer} or {@code X-Agent-Token}, else the same opaque 404 as any other path.
 */
public class McpHandler extends AgentHandler
{
	public static final String PATH = "/mcp";
	private static final int MAX_BODY = 1024 * 1024;

	private final McpProtocol protocol;

	public McpHandler(Gson gson, McpProtocol protocol)
	{
		super(gson);
		this.protocol = protocol;
	}

	public McpProtocol getProtocol()
	{
		return protocol;
	}

	@Override
	public String getPath()
	{
		return PATH;
	}

	@Override
	protected void handleRequest(HttpExchange exchange) throws IOException
	{
		String sub = getSubPath(exchange, PATH);
		if (!sub.isEmpty() && !"/".equals(sub))
		{
			sendOpaqueNotFound(exchange);
			return;
		}

		String method = exchange.getRequestMethod().toUpperCase();
		String session = exchange.getRequestHeaders().getFirst(McpProtocol.SESSION_HEADER);
		McpProtocol.Reply reply;
		switch (method)
		{
			case "POST":
			{
				byte[] body = exchange.getRequestBody().readNBytes(MAX_BODY + 1);
				if (body.length > MAX_BODY)
				{
					write(exchange, 413, "{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32600,\"message\":\"Request body too large\"}}", null);
					return;
				}
				reply = protocol.handlePost(new String(body, StandardCharsets.UTF_8), session,
					exchange.getRequestHeaders().getFirst(McpProtocol.PROTOCOL_HEADER));
				break;
			}
			case "DELETE":
				reply = protocol.handleDelete(session);
				break;
			default:
				exchange.getResponseHeaders().set("Allow", "POST, DELETE");
				write(exchange, 405, "{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32600,\"message\":\"Use POST. This server does not open server-initiated streams.\"}}", null);
				return;
		}
		write(exchange, reply.status, reply.body, reply.headers);
	}

	private static void write(HttpExchange exchange, int status, String body, Map<String, String> headers) throws IOException
	{
		if (headers != null)
		{
			for (Map.Entry<String, String> h : headers.entrySet())
			{
				exchange.getResponseHeaders().set(h.getKey(), h.getValue());
			}
		}
		if (body == null)
		{
			exchange.sendResponseHeaders(status, -1);
			exchange.close();
			return;
		}
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json");
		exchange.sendResponseHeaders(status, bytes.length);
		try (OutputStream os = exchange.getResponseBody())
		{
			os.write(bytes);
		}
	}
}
