package net.runelite.client.plugins.projectx.agentserver.handler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpServer;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.api.playerstate.Rs2PlayerStateCache;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VarpHandlerTest {

	private static final Gson GSON = new GsonBuilder().create();
	private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

	private static final int VARP_COUNT = 5996;

	private static HttpServer server;
	private static int port;
	private static Field cacheField;
	private static Object previousCache;

	@BeforeClass
	public static void startServer() throws Exception {
		Client client = mock(Client.class);
		ClientThread clientThread = mock(ClientThread.class);
		when(clientThread.runOnClientThreadOptional(any())).thenAnswer(invocation -> {
			Callable<?> callable = invocation.getArgument(0);
			return Optional.ofNullable(callable.call());
		});
		when(client.getVarps()).thenReturn(new int[VARP_COUNT]);
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(client.getVarpValue(281)).thenReturn(1000);

		cacheField = ProjectX.class.getDeclaredField("rs2PlayerStateCache");
		cacheField.setAccessible(true);
		previousCache = cacheField.get(null);
		cacheField.set(null, new Rs2PlayerStateCache(new EventBus(), client, clientThread));

		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.setExecutor(Executors.newFixedThreadPool(2));

		VarpHandler handler = new VarpHandler(GSON);
		server.createContext(handler.getPath(), handler);
		server.start();

		port = server.getAddress().getPort();
	}

	@AfterClass
	public static void stopServer() throws IllegalAccessException {
		if (server != null) {
			server.stop(0);
		}
		if (cacheField != null) {
			cacheField.set(null, previousCache);
		}
	}

	@Test
	public void testNegativeIdReturns400() throws IOException {
		Response resp = get("/varp?id=-1");
		assertEquals(400, resp.code);
		assertTrue(((String) resp.body.get("error")).toLowerCase().contains("invalid varp id"));
	}

	@Test
	public void testOutOfRangeIdReturns400() throws IOException {
		Response resp = get("/varp?id=8063");
		assertEquals(400, resp.code);
		assertTrue(((String) resp.body.get("error")).toLowerCase().contains("invalid varp id"));

		Response edge = get("/varp?id=" + VARP_COUNT);
		assertEquals(400, edge.code);
	}

	@Test
	public void testValidIdReturnsValue() throws IOException {
		Response resp = get("/varp?id=281");
		assertEquals(200, resp.code);
		assertEquals(281.0, resp.body.get("varpId"));
		assertEquals(1000.0, resp.body.get("value"));
	}

	@Test
	public void testMissingIdReturns400() throws IOException {
		Response resp = get("/varp");
		assertEquals(400, resp.code);
		assertTrue(((String) resp.body.get("error")).toLowerCase().contains("id"));
	}

	@Test
	public void testEmptyIdReturns400() throws IOException {
		Response resp = get("/varp?id=");
		assertEquals(400, resp.code);
		assertNotNull(resp.body.get("error"));
	}

	@Test
	public void testNonNumericIdReturns400() throws IOException {
		Response resp = get("/varp?id=abc");
		assertEquals(400, resp.code);
		assertTrue(((String) resp.body.get("error")).toLowerCase().contains("invalid varp id"));
	}

	@Test
	public void testPostReturns405() throws IOException {
		HttpURLConnection conn = openConnection("/varp?id=281");
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setDoOutput(true);
		try (OutputStream os = conn.getOutputStream()) {
			os.write("{}".getBytes(StandardCharsets.UTF_8));
		}
		assertEquals(405, conn.getResponseCode());
	}

	@Test
	public void testCrossOriginRejected() throws IOException, InterruptedException {
		java.net.http.HttpClient hc = java.net.http.HttpClient.newHttpClient();
		java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
				.uri(java.net.URI.create("http://127.0.0.1:" + port + "/varp?id=281"))
				.header("Origin", "https://evil.example.com")
				.GET()
				.build();
		java.net.http.HttpResponse<String> resp = hc.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
		// Opaque 404 for all pre-auth failures — scanners must not distinguish a real endpoint.
		assertEquals(404, resp.statusCode());
	}

	@Test
	public void testPathIsVarp() {
		assertEquals("/varp", new VarpHandler(GSON).getPath());
	}

	private Response get(String path) throws IOException {
		HttpURLConnection conn = openConnection(path);
		conn.setRequestMethod("GET");
		return readResponse(conn);
	}

	private HttpURLConnection openConnection(String path) throws IOException {
		URL url = new URL("http://127.0.0.1:" + port + path);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(5000);
		conn.setReadTimeout(5000);
		return conn;
	}

	private Response readResponse(HttpURLConnection conn) throws IOException {
		int code = conn.getResponseCode();
		InputStream stream = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
		String json;
		try (Scanner sc = new Scanner(stream, StandardCharsets.UTF_8.name())) {
			json = sc.useDelimiter("\\A").hasNext() ? sc.next() : "{}";
		}
		Map<String, Object> body = GSON.fromJson(json, MAP_TYPE);
		return new Response(code, body);
	}

	private static class Response {
		final int code;
		final Map<String, Object> body;

		Response(int code, Map<String, Object> body) {
			this.code = code;
			this.body = body;
		}
	}
}
