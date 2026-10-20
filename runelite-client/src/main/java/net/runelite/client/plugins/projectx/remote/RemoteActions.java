package net.runelite.client.plugins.projectx.remote;

/**
 * What the website can ask this client to do. Each returns what happened in
 * plain words, or throws {@link Refused} saying why not. Called on the remote
 * thread, never the client thread or the Swing thread, so each may wait for
 * either.
 *
 * <p>Split from {@link RemoteDispatcher} so the dispatch rules can be tested with
 * a fake; {@link LiveRemoteActions} is the real one.
 */
public interface RemoteActions
{
    String startScript(String internalName) throws Refused;

    String stopScript(String internalName) throws Refused;

    String stopAll() throws Refused;

    String startQueue(String plan) throws Refused;

    String stopQueue() throws Refused;

    String startBreak() throws Refused;

    String endBreak() throws Refused;

    String logout() throws Refused;

    /** A command that could not be carried out, and why, for the Control Panel. */
    class Refused extends Exception
    {
        public Refused(String message)
        {
            super(message, null, false, false);
        }
    }
}
