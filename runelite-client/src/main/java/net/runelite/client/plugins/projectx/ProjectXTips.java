package net.runelite.client.plugins.projectx;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * The lines shown under "Did you know?" while the client starts.
 *
 * <p>These used to be fetched from {@code /api/fact/random}, an endpoint the
 * site does not have, so every start-up showed "Failed to fetch random fact" and
 * then asked again twenty seconds later. A splash screen is the worst possible
 * place for a network round trip: it is on screen for a few seconds, usually
 * before anything else has warmed up, and the one thing it must never do is
 * wait.
 *
 * <p>So the lines live here instead. They cost nothing, work offline, and can
 * say something true about this client rather than a generic fact about octopus
 * hearts. Roughly two thirds are worth knowing; the rest are there because a
 * loading screen is a good place to make somebody smile.
 */
public final class ProjectXTips
{
	private static final List<String> TIPS = List.of(
		// ---- worth knowing
		"Anything you buy arrives on its own, usually within a minute. There is nothing to copy into a folder.",
		"Buy again before your time runs out and the days are added to what you have, not replaced.",
		"Nothing renews itself. No card is kept on file, and there is nothing to remember to cancel.",
		"Every script is checked against its published fingerprint before it loads. If the bytes disagree, it does not run.",
		"Your access follows your account, not this computer. Sign in elsewhere and it is all still there.",
		"In the Hub, a green stripe means installed. An orange one means the author has published something newer.",
		"Antiban settings are shared across scripts, so breaks line up instead of fighting each other.",
		"One X Token is one dollar, and tokens do not expire. Spend them whenever you like.",
		"You install the launcher once. After that it updates itself, and keeps the client current for you.",
		"Most of the Hub is free. The store is for the scripts people charge for.",
		"Scripts update themselves too. When an author ships a fix, your client moves to it without being asked.",
		"Every script in the store was submitted with its source, and somebody read it before it went on sale.",
		"Running several accounts at once needs instances. Everyone gets one free.",
		"The developer portal is open to anyone. Publish a script and keep 70% of every sale.",

		// ---- not worth knowing
		"Tree spirits guard the yews for a reason. That reason is you.",
		"Nobody in the history of Gielinor has said \"one more Zulrah kill\" and meant it.",
		"The Grand Exchange is the only place where standing still counts as an activity.",
		"Statistically, the rarest drop in the game is a tidy bank tab.",
		"The Wilderness ditch has ended more accounts than any boss in the game.",
		"Do not alch the party hat. Not even to see what happens.",
		"If somebody offers to double your gold, you have not met a scammer. You have met a tradition.",
		"Draynor willows have seen things they will never speak of.",
		"Somewhere right now, a bond is funding a skill that will be abandoned by Thursday.",
		"Fishing is the only skill where doing nothing for six hours is the intended experience.",
		"Every veteran has one quest they have never finished and will defend to the death."
	);

	/**
	 * The order the tips are shown in, reshuffled once every one has had a turn.
	 *
	 * <p>Not simply picked at random each time: the splash screen ignores a line
	 * identical to the one already up, so an unlucky repeat would leave the same
	 * text sitting there for forty seconds and look broken. Walking a shuffled
	 * list means nothing repeats until everything has been seen.
	 */
	private static final List<String> order = new ArrayList<>();
	private static int next;

	private ProjectXTips()
	{
	}

	public static synchronized String nextTip()
	{
		if (next >= order.size())
		{
			List<String> reshuffled = new ArrayList<>(TIPS);
			Collections.shuffle(reshuffled);

			// Do not open the new pass with the line already on screen.
			if (!order.isEmpty() && reshuffled.get(0).equals(order.get(order.size() - 1)) && reshuffled.size() > 1)
			{
				Collections.swap(reshuffled, 0, 1);
			}

			order.clear();
			order.addAll(reshuffled);
			next = 0;
		}

		return order.get(next++);
	}

	/**
	 * Hands the next tip to the splash screen.
	 *
	 * <p>Keeps the shape the caller already had, including handing the value over
	 * on the event dispatch thread: the callback ends up changing a label, and
	 * the scheduler this is called from is not the EDT. There is no telemetry
	 * switch to respect any more, because nothing leaves the machine.
	 */
	public static void nextTip(Consumer<String> callback)
	{
		String tip = nextTip();
		SwingUtilities.invokeLater(() -> callback.accept(tip));
	}
}
