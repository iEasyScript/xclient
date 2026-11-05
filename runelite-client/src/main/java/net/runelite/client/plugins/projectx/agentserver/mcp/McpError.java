package net.runelite.client.plugins.projectx.agentserver.mcp;

/**
 * A JSON-RPC protocol error: the request itself was wrong (unknown method, unknown tool, bad
 * arguments). Becomes a JSON-RPC {@code error} object. A tool that ran and failed throws
 * {@link ToolFailure} instead, which becomes a tool result with {@code isError: true} so the
 * model can read why.
 */
public class McpError extends RuntimeException
{
	public static final int PARSE_ERROR = -32700;
	public static final int INVALID_REQUEST = -32600;
	public static final int METHOD_NOT_FOUND = -32601;
	public static final int INVALID_PARAMS = -32602;
	public static final int INTERNAL_ERROR = -32603;

	private final int code;

	public McpError(int code, String message)
	{
		super(message);
		this.code = code;
	}

	public int getCode()
	{
		return code;
	}

	public static McpError invalidParams(String message)
	{
		return new McpError(INVALID_PARAMS, message);
	}
}
