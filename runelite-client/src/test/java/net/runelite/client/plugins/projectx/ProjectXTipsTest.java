package net.runelite.client.plugins.projectx;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * The splash screen drops a tip identical to the one already showing, so a
 * repeat is not merely dull -- it leaves the same line on screen for another
 * twenty seconds and looks like the client has hung.
 */
public class ProjectXTipsTest
{
	/** Long enough to cross several reshuffles. */
	private static final int DRAWS = 400;

	@Test
	public void neverRepeatsBackToBack()
	{
		String previous = null;
		for (int i = 0; i < DRAWS; i++)
		{
			String tip = ProjectXTips.nextTip();
			assertNotEquals("tip " + i + " repeated the one before it", previous, tip);
			previous = tip;
		}
	}

	@Test
	public void showsEveryTipBeforeShowingAnyTwice()
	{
		// Drain whatever is left of the pass in progress, then watch one whole
		// pass: within it, nothing may appear twice.
		Set<String> everything = new HashSet<>();
		for (int i = 0; i < DRAWS; i++)
		{
			everything.add(ProjectXTips.nextTip());
		}

		int total = everything.size();
		assertTrue("expected a decent spread of tips, got " + total, total >= 20);

		List<String> pass = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		for (int i = 0; i < total; i++)
		{
			String tip = ProjectXTips.nextTip();
			pass.add(tip);
			seen.add(tip);
		}

		assertEquals("a tip appeared twice within one pass: " + pass, pass.size(), seen.size());
	}

	@Test
	public void tipsAreUsableText()
	{
		Set<String> seen = new HashSet<>();
		for (int i = 0; i < DRAWS; i++)
		{
			String tip = ProjectXTips.nextTip();
			assertFalse("a tip was blank", tip.trim().isEmpty());
			// The splash screen escapes these into HTML; a stray tag would show
			// as literal text or break the layout.
			assertFalse("a tip contained markup: " + tip, tip.contains("<") || tip.contains(">"));
			seen.add(tip);
		}
		assertFalse(seen.isEmpty());
	}
}
