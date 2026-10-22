package net.runelite.client.plugins.projectx.util.events;

import java.util.function.BooleanSupplier;
import net.runelite.client.plugins.projectx.ProjectX;

final class ClientThreadProbe
{
	static final long VALIDATE_TIMEOUT_MILLIS = 1000;

	private ClientThreadProbe()
	{
	}

	static boolean isVisible(BooleanSupplier onClientThread)
	{
		if (!ProjectX.isLoggedIn())
		{
			return false;
		}
		return ProjectX.getClientThread()
			.runOnClientThreadOptional(onClientThread::getAsBoolean, VALIDATE_TIMEOUT_MILLIS)
			.orElse(false);
	}
}
