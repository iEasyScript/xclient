package net.runelite.client.plugins.projectx.agentserver.mcp;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A tool ran but could not do what was asked (nothing found, game thread busy, not logged in).
 * Reported as a tool result with {@code isError: true}; the payload is shown to the model.
 */
public class ToolFailure extends RuntimeException
{
	private final JsonElement payload;

	public ToolFailure(String message)
	{
		this(message, null);
	}

	public ToolFailure(String message, JsonElement payload)
	{
		super(message);
		if (payload == null)
		{
			JsonObject o = new JsonObject();
			o.addProperty("error", message);
			payload = o;
		}
		this.payload = payload;
	}

	public JsonElement getPayload()
	{
		return payload;
	}
}
