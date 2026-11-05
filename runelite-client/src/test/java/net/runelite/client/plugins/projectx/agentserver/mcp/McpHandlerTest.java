package net.runelite.client.plugins.projectx.agentserver.mcp;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import net.runelite.client.plugins.projectx.agentserver.handler.AgentHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** /mcp over real HTTP: auth (Bearer and X-Agent-Token), opaque 404 on failure, methods. */
public class McpHandlerTest
{
	private static final String TOKEN = "secret-token";
	private static final String INIT = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2025-06-18\"}}";

	private HttpServer server;
	private int port;

	@Before
	public void setUp() throws IOException
	{
		McpToolRegistry registry = new McpToolRegistry().category("test");
		registry.add("hello", "Says hello. Used by tests.", Schema.empty(), a -> ToolResult.text("hello"));
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext(McpHandler.PATH, new McpHandler(new Gson(), new McpProtocol(registry, "projectx-osrs", "test")));
		AgentHandler.setTokenSupplier(() -> TOKEN);
		server.start();
		port = server.getAddress().getPort();
	}

	@After
	public void tearDown()
	{
		if (server != null)
		{
			server.stop(0);
		}
		AgentHandler.setTokenSupplier(null);
	}

	private HttpURLConnection open(String method, String header, String value) throws IOException
	{
		HttpURLConnection conn = (HttpURLConnection) new URL("http://127.0.0.1:" + port + "/mcp").openConnection();
		conn.setRequestMethod(method);
		conn.setRequestProperty("Accept", "application/json, text/event-stream");
		if (header != null)
		{
			conn.setRequestProperty(header, value);
		}
		return conn;
	}

	private static void send(HttpURLConnection conn, String body) throws IOException
	{
		conn.setDoOutput(true);
		conn.setRequestProperty("Content-Type", "application/json");
		try (OutputStream os = conn.getOutputStream())
		{
			os.write(body.getBytes(StandardCharsets.UTF_8));
		}
	}

	private static String read(HttpURLConnection conn) throws IOException
	{
		InputStream in = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream();
		return in == null ? "" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
	}

	@Test
	public void missingTokenIsOpaque404() throws Exception
	{
		HttpURLConnection conn = open("POST", null, null);
		send(conn, INIT);
		assertEquals(404, conn.getResponseCode());
		assertEquals("Not Found", read(conn));
	}

	@Test
	public void wrongBearerTokenIsOpaque404() throws Exception
	{
		HttpURLConnection conn = open("POST", "Authorization", "Bearer wrong");
		send(conn, INIT);
		assertEquals(404, conn.getResponseCode());
		assertEquals("Not Found", read(conn));
	}

	@Test
	public void crossOriginRequestIsOpaque404() throws Exception
	{
		// HttpURLConnection drops a caller-set Origin header, so write the request by hand.
		byte[] body = INIT.getBytes(StandardCharsets.UTF_8);
		try (java.net.Socket socket = new java.net.Socket("127.0.0.1", port))
		{
			socket.getOutputStream().write(("POST /mcp HTTP/1.1\r\n"
				+ "Host: 127.0.0.1:" + port + "\r\n"
				+ "Origin: https://evil.example.com\r\n"
				+ "Authorization: Bearer " + TOKEN + "\r\n"
				+ "Content-Type: application/json\r\n"
				+ "Content-Length: " + body.length + "\r\n"
				+ "Connection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8));
			socket.getOutputStream().write(body);
			socket.getOutputStream().flush();
			String response = new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
			assertEquals("HTTP/1.1 404 Not Found", response.split("\r\n", 2)[0]);
		}
	}

	@Test
	public void bearerTokenInitializes() throws Exception
	{
		HttpURLConnection conn = open("POST", "Authorization", "Bearer " + TOKEN);
		send(conn, INIT);
		assertEquals(200, conn.getResponseCode());
		assertTrue(conn.getContentType().startsWith("application/json"));
		assertNotNull(conn.getHeaderField(McpProtocol.SESSION_HEADER));
		JsonObject res = new JsonParser().parse(read(conn)).getAsJsonObject();
		assertEquals("2025-06-18", res.getAsJsonObject("result").get("protocolVersion").getAsString());
	}

	@Test
	public void agentTokenHeaderAlsoWorks() throws Exception
	{
		HttpURLConnection conn = open("POST", "X-Agent-Token", TOKEN);
		send(conn, "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/call\",\"params\":{\"name\":\"hello\"}}");
		assertEquals(200, conn.getResponseCode());
		assertTrue(read(conn).contains("hello"));
	}

	@Test
	public void notificationGets202() throws Exception
	{
		HttpURLConnection conn = open("POST", "Authorization", "Bearer " + TOKEN);
		send(conn, "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}");
		assertEquals(202, conn.getResponseCode());
	}

	@Test
	public void getIs405() throws Exception
	{
		HttpURLConnection conn = open("GET", "Authorization", "Bearer " + TOKEN);
		assertEquals(405, conn.getResponseCode());
	}

	@Test
	public void deleteEndsSession() throws Exception
	{
		HttpURLConnection conn = open("DELETE", "Authorization", "Bearer " + TOKEN);
		conn.setRequestProperty(McpProtocol.SESSION_HEADER, "abc");
		assertEquals(204, conn.getResponseCode());
	}
}
