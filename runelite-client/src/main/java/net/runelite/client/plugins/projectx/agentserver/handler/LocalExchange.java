package net.runelite.client.plugins.projectx.agentserver.handler;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * An in-memory {@link HttpExchange}: the request is given up front and the handler's answer is
 * captured instead of written to a socket. Used by {@link AgentHandler#invokeLocally} so MCP tools
 * can call the REST handlers in-process.
 */
public final class LocalExchange extends HttpExchange {

	private static final InetSocketAddress LOOPBACK = new InetSocketAddress("127.0.0.1", 0);

	private final String method;
	private final URI uri;
	private final Headers requestHeaders = new Headers();
	private final Headers responseHeaders = new Headers();
	private final Map<String, Object> attributes = new HashMap<>();
	private InputStream requestBody;
	private ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
	private OutputStream responseStream = responseBody;
	private int responseCode = -1;

	LocalExchange(String method, URI uri, byte[] body) {
		this.method = method;
		this.uri = uri;
		this.requestBody = new ByteArrayInputStream(body);
		requestHeaders.set("Host", "127.0.0.1");
		requestHeaders.set("Content-Type", "application/json");
		requestHeaders.set("Content-Length", String.valueOf(body.length));
	}

	/** The captured answer. */
	public static final class Response {
		private final int status;
		private final String contentType;
		private final byte[] body;

		Response(int status, String contentType, byte[] body) {
			this.status = status;
			this.contentType = contentType;
			this.body = body;
		}

		public int getStatus() {
			return status;
		}

		public String getContentType() {
			return contentType;
		}

		public byte[] getBody() {
			return body;
		}

		public boolean isJson() {
			return contentType != null && contentType.startsWith("application/json");
		}

		public JsonElement json() {
			return new JsonParser().parse(new String(body, StandardCharsets.UTF_8));
		}
	}

	Response response() {
		return new Response(responseCode, responseHeaders.getFirst("Content-Type"), responseBody.toByteArray());
	}

	@Override
	public Headers getRequestHeaders() {
		return requestHeaders;
	}

	@Override
	public Headers getResponseHeaders() {
		return responseHeaders;
	}

	@Override
	public URI getRequestURI() {
		return uri;
	}

	@Override
	public String getRequestMethod() {
		return method;
	}

	@Override
	public HttpContext getHttpContext() {
		return null;
	}

	@Override
	public void close() {
	}

	@Override
	public InputStream getRequestBody() {
		return requestBody;
	}

	@Override
	public OutputStream getResponseBody() {
		return responseStream;
	}

	@Override
	public void sendResponseHeaders(int rCode, long responseLength) {
		this.responseCode = rCode;
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		return LOOPBACK;
	}

	@Override
	public int getResponseCode() {
		return responseCode;
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		return LOOPBACK;
	}

	@Override
	public String getProtocol() {
		return "HTTP/1.1";
	}

	@Override
	public Object getAttribute(String name) {
		return attributes.get(name);
	}

	@Override
	public void setAttribute(String name, Object value) {
		attributes.put(name, value);
	}

	@Override
	public void setStreams(InputStream i, OutputStream o) {
		if (i != null) {
			requestBody = i;
		}
		if (o != null) {
			responseStream = o;
		}
	}

	@Override
	public HttpPrincipal getPrincipal() {
		return null;
	}
}
