package net.runelite.client.plugins.projectx.questhelper.steps.choice;

import org.junit.Test;

import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Covers the matching that decides which dialogue option a quest step presses.
 *
 * <p>Worth pinning down, because the quest helper used to reach an option only through the
 * "[n]" marker the highlighter writes into it -- so a quest stood at its NPC clicking the
 * ground forever whenever that marker was absent. These cases are the widget text the game
 * actually hands over, markup and all.
 */
public class WidgetChoiceStepTest
{
	/** The config is only read when drawing a highlight, never when matching. */
	private static DialogChoiceStep choice(String text)
	{
		return new DialogChoiceStep(null, text);
	}

	@Test
	public void matchesPlainOptionText()
	{
		assertTrue(choice("Yes.").matches("Yes."));
		assertTrue(choice("I'm looking for a quest.").matches("I'm looking for a quest."));
	}

	@Test
	public void matchesThroughColourTags()
	{
		assertTrue(choice("Yes.").matches("<col=0000ff>Yes.</col>"));
		assertTrue(choice("What's wrong?").matches("<col=ffffff>What's wrong?"));
	}

	@Test
	public void matchesAnOptionItHasAlreadyNumbered()
	{
		// highlightText() rewrites the option in place, so the text the script later reads is
		// not the text the quest declared. Matching has to survive its own highlight.
		assertTrue(choice("Yes, okay. I can do that.").matches("[2] Yes, okay. I can do that."));
		assertTrue(choice("Yes.").matches("[1] <col=0000ff>Yes."));
	}

	@Test
	public void matchesThroughTypographicApostrophes()
	{
		assertTrue(choice("He's got a ghost haunting his graveyard.")
			.matches("He’s got a ghost haunting his graveyard."));
	}

	@Test
	public void doesNotMatchADifferentOption()
	{
		assertFalse(choice("Yes.").matches("No."));
		assertFalse(choice("Can I help?").matches("What's wrong?"));
	}

	@Test
	public void ignoresCase()
	{
		// The Restless Ghost declares this line twice, once with "WHY" and once with "why",
		// which is the quest data saying it is not sure how the game capitalises it. Matching
		// case-insensitively is what lets either declaration answer the prompt on screen.
		assertTrue(choice("Yes, ok. Do you know WHY you're a ghost?")
			.matches("Yes, ok. Do you know why you're a ghost?"));
		assertTrue(choice("Yes, ok. Do you know why you're a ghost?")
			.matches("Yes, ok. Do you know WHY you're a ghost?"));
	}

	@Test
	public void matchesByPattern()
	{
		DialogChoiceStep pattern = new DialogChoiceStep(null, Pattern.compile("shearing these sheep"));
		assertTrue(pattern.matches("I need to talk to you about shearing these sheep!"));
		assertTrue(pattern.matches("[3] I need to talk to you about shearing these sheep!"));
		assertFalse(pattern.matches("Yes."));
	}

	@Test
	public void survivesTextItCannotMatch()
	{
		// An index-based choice names an option by position and has no wording to compare, and
		// null is what the game hands back for an option slot it has not filled in.
		assertFalse(new DialogChoiceStep(null, 2).matches("Yes."));
		assertFalse(choice("Yes.").matches(null));
	}
}
