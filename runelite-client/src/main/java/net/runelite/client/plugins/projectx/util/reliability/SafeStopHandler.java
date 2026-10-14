package net.runelite.client.plugins.projectx.util.reliability;

/**
 * Stops the script and tells the player why. For a {@code Script} this shows the message
 * with {@code ProjectX.showMessage} (which also becomes the reason in the stop alert) and
 * then calls {@code shutdown()}, the same way scripts already stop themselves.
 */
@FunctionalInterface
public interface SafeStopHandler
{
    void stop(String userMessage);
}
