package net.runelite.client.plugins.projectx.agentserver.mcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** The JSON-RPC / MCP layer, without HTTP. */
public class McpProtocolTest
{
	private McpProtocol protocol;

	@Before
	public void setUp()
	{
		McpToolRegistry registry = new McpToolRegistry().category("test");
		registry.add("echo", "Echo the text back. Used by tests.",
			Schema.object().str("text", "Text to echo").integer("times", "Repeat count").required("text").build(),
			a ->
			{
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < a.integer("times", 1); i++)
				{
					sb.append(a.requireStr("text"));
				}
				JsonObject o = new JsonObject();
				o.addProperty("text", sb.toString());
				return ToolResult.json(o);
			});
		registry.add("fail", "Always fails as a tool error.", Schema.empty(),
			a ->
			{
				throw new ToolFailure("nothing to do");
			});
		registry.add("boom", "Throws an unexpected exception.", Schema.empty(),
			a ->
			{
				throw new IllegalStateException("kaput");
			});
		protocol = new McpProtocol(registry, "projectx-osrs", "9.9.9");
	}

	private static JsonObject body(McpProtocol.Reply r)
	{
		return new JsonParser().parse(r.body).getAsJsonObject();
	}

	private McpProtocol.Reply post(String json)
	{
		return protocol.handlePost(json, null, null);
	}

	private static String call(int id, String tool, String args)
	{
		return "{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"method\":\"tools/call\",\"params\":{\"name\":\"" + tool + "\""
			+ (args != null ? ",\"arguments\":" + args : "") + "}}";
	}

	@Test
	public void initializeNegotiatesVersionAndIssuesSession()
	{
		McpProtocol.Reply r = post("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{"
			+ "\"protocolVersion\":\"2025-06-18\",\"capabilities\":{},\"clientInfo\":{\"name\":\"t\",\"version\":\"1\"}}}");
		assertEquals(200, r.status);
		JsonObject res = body(r);
		assertEquals("2.0", res.get("jsonrpc").getAsString());
		assertEquals(1, res.get("id").getAsInt());
		JsonObject result = res.getAsJsonObject("result");
		assertEquals("2025-06-18", result.get("protocolVersion").getAsString());
		assertTrue(result.getAsJsonObject("capabilities").has("tools"));
		assertEquals("projectx-osrs", result.getAsJsonObject("serverInfo").get("name").getAsString());
		assertEquals("9.9.9", result.getAsJsonObject("serverInfo").get("version").getAsString());
		String session = r.headers.get(McpProtocol.SESSION_HEADER);
		assertNotNull(session);
		assertTrue(protocol.hasSession(session));
	}

	@Test
	public void initializeAccepts20250326AndFallsBackForUnknownVersions()
	{
		JsonObject old = body(post("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2025-03-26\"}}"));
		assertEquals("2025-03-26", old.getAsJsonObject("result").get("protocolVersion").getAsString());
		JsonObject future = body(post("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2099-01-01\"}}"));
		assertEquals(McpProtocol.LATEST_PROTOCOL, future.getAsJsonObject("result").get("protocolVersion").getAsString());
	}

	@Test
	public void notificationsGet202WithNoBody()
	{
		McpProtocol.Reply r = post("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}");
		assertEquals(202, r.status);
		assertNull(r.body);
		assertEquals(202, post("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/cancelled\",\"params\":{\"requestId\":3}}").status);
	}

	@Test
	public void pingReturnsEmptyResult()
	{
		JsonObject res = body(post("{\"jsonrpc\":\"2.0\",\"id\":\"p1\",\"method\":\"ping\"}"));
		assertEquals("p1", res.get("id").getAsString());
		assertEquals(0, res.getAsJsonObject("result").size());
	}

	@Test
	public void toolsListHasNameDescriptionAndSchema()
	{
		JsonArray tools = body(post("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}"))
			.getAsJsonObject("result").getAsJsonArray("tools");
		assertEquals(3, tools.size());
		for (JsonElement e : tools)
		{
			JsonObject t = e.getAsJsonObject();
			assertTrue(t.has("name"));
			assertTrue(t.has("description"));
			assertEquals("object", t.getAsJsonObject("inputSchema").get("type").getAsString());
		}
	}

	@Test
	public void toolsCallReturnsTextContent()
	{
		JsonObject result = body(post(call(5, "echo", "{\"text\":\"ab\",\"times\":2}"))).getAsJsonObject("result");
		assertFalse(result.get("isError").getAsBoolean());
		JsonObject content = result.getAsJsonArray("content").get(0).getAsJsonObject();
		assertEquals("text", content.get("type").getAsString());
		assertEquals("abab", new JsonParser().parse(content.get("text").getAsString()).getAsJsonObject().get("text").getAsString());
	}

	@Test
	public void unknownMethodIsMethodNotFound()
	{
		JsonObject res = body(post("{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"resources/list\"}"));
		assertEquals(McpError.METHOD_NOT_FOUND, res.getAsJsonObject("error").get("code").getAsInt());
		assertEquals(7, res.get("id").getAsInt());
	}

	@Test
	public void unknownToolIsInvalidParams()
	{
		JsonObject res = body(post(call(8, "nope", "{}")));
		assertEquals(McpError.INVALID_PARAMS, res.getAsJsonObject("error").get("code").getAsInt());
	}

	@Test
	public void missingRequiredArgumentIsInvalidParams()
	{
		JsonObject res = body(post(call(9, "echo", "{}")));
		assertEquals(McpError.INVALID_PARAMS, res.getAsJsonObject("error").get("code").getAsInt());
		assertTrue(res.getAsJsonObject("error").get("message").getAsString().contains("text"));
	}

	@Test
	public void wrongArgumentTypeIsInvalidParams()
	{
		JsonObject res = body(post(call(10, "echo", "{\"text\":\"a\",\"times\":\"lots\"}")));
		assertEquals(McpError.INVALID_PARAMS, res.getAsJsonObject("error").get("code").getAsInt());
	}

	@Test
	public void argumentsMustBeAnObject()
	{
		JsonObject res = body(post(call(11, "echo", "[1,2]")));
		assertEquals(McpError.INVALID_PARAMS, res.getAsJsonObject("error").get("code").getAsInt());
	}

	@Test
	public void paramsMustBeAnObject()
	{
		JsonObject res = body(post("{\"jsonrpc\":\"2.0\",\"id\":12,\"method\":\"tools/list\",\"params\":5}"));
		assertEquals(McpError.INVALID_PARAMS, res.getAsJsonObject("error").get("code").getAsInt());
	}

	@Test
	public void toolFailureIsAnErrorResultNotAProtocolError()
	{
		JsonObject res = body(post(call(13, "fail", null)));
		assertFalse(res.has("error"));
		JsonObject result = res.getAsJsonObject("result");
		assertTrue(result.get("isError").getAsBoolean());
		assertTrue(result.getAsJsonArray("content").get(0).getAsJsonObject().get("text").getAsString().contains("nothing to do"));
	}

	@Test
	public void unexpectedExceptionIsAnErrorResult()
	{
		JsonObject result = body(post(call(14, "boom", null))).getAsJsonObject("result");
		assertTrue(result.get("isError").getAsBoolean());
		assertTrue(result.getAsJsonArray("content").get(0).getAsJsonObject().get("text").getAsString().contains("kaput"));
	}

	@Test
	public void malformedJsonIsParseError()
	{
		McpProtocol.Reply r = post("{not json");
		assertEquals(400, r.status);
		JsonObject res = body(r);
		assertEquals(McpError.PARSE_ERROR, res.getAsJsonObject("error").get("code").getAsInt());
		assertTrue(res.get("id").isJsonNull());
	}

	@Test
	public void wrongJsonRpcVersionIsInvalidRequest()
	{
		McpProtocol.Reply r = post("{\"jsonrpc\":\"1.0\",\"id\":1,\"method\":\"ping\"}");
		assertEquals(400, r.status);
		assertEquals(McpError.INVALID_REQUEST, body(r).getAsJsonObject("error").get("code").getAsInt());
	}

	@Test
	public void unsupportedProtocolHeaderIsRejected()
	{
		McpProtocol.Reply r = protocol.handlePost("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}", null, "1999-01-01");
		assertEquals(400, r.status);
		assertEquals(200, protocol.handlePost("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}", null, "2025-06-18").status);
	}

	@Test
	public void batchesAnswerEachRequestAndSkipNotifications()
	{
		McpProtocol.Reply r = post("[{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"},"
			+ "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"},"
			+ "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"nope\"}]");
		assertEquals(200, r.status);
		JsonArray answers = new JsonParser().parse(r.body).getAsJsonArray();
		assertEquals(2, answers.size());
	}

	@Test
	public void clientResponsesAreAccepted()
	{
		assertEquals(202, post("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{}}").status);
	}

	@Test
	public void deleteEndsSession()
	{
		McpProtocol.Reply init = post("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}");
		String session = init.headers.get(McpProtocol.SESSION_HEADER);
		assertEquals(204, protocol.handleDelete(session).status);
		assertFalse(protocol.hasSession(session));
	}

	@Test
	public void snakeCaseRenamesOnlyCamelCaseKeys()
	{
		JsonObject in = new JsonParser().parse("{\"groupId\":1,\"net.example.FooPlugin\":{\"loopCount\":2},\"x\":3,\"QUEST_ENUM\":4}").getAsJsonObject();
		JsonObject out = McpJson.snake(in).getAsJsonObject();
		assertTrue(out.has("group_id"));
		assertTrue(out.has("net.example.FooPlugin"));
		assertTrue(out.getAsJsonObject("net.example.FooPlugin").has("loop_count"));
		assertTrue(out.has("x"));
		assertTrue(out.has("QUEST_ENUM"));
	}
}
