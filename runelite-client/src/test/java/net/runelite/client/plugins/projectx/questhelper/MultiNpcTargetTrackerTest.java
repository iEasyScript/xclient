package net.runelite.client.plugins.projectx.questhelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.runelite.client.plugins.projectx.questhelper.steps.QuestStep;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

public class MultiNpcTargetTrackerTest
{
	private static final int ANNE = 11;
	private static final int WILL = 22;

	private MultiNpcTargetTracker tracker;
	private QuestStep talkToWillOrAnne;
	private Set<Integer> handled;
	private Comparator<Integer> nearestFirst;

	@Before
	public void setUp()
	{
		tracker = new MultiNpcTargetTracker();
		talkToWillOrAnne = mock(QuestStep.class);
		handled = new HashSet<>();
		nearestFirst = Comparator.comparingInt(index -> index == WILL ? 1 : 5);
	}

	private Integer select(QuestStep step, List<Integer> npcs, long now)
	{
		return tracker.select(step, npcs, Integer::intValue, handled, nearestFirst, now);
	}

	@Test
	public void picksFirstUnhandledWhenNothingCommitted()
	{
		assertEquals(Integer.valueOf(ANNE), select(talkToWillOrAnne, Arrays.asList(ANNE, WILL), 0));
	}

	@Test
	public void keepsCommittedNpcAfterItWasHandled()
	{
		tracker.commit(talkToWillOrAnne, ANNE, 0);
		handled.add(ANNE);

		assertEquals(Integer.valueOf(ANNE), select(talkToWillOrAnne, Arrays.asList(ANNE, WILL), 1_000));
		assertEquals(Integer.valueOf(ANNE), select(talkToWillOrAnne, Arrays.asList(WILL, ANNE), 2_000));
	}

	@Test
	public void keepsCommittedNpcWhenAnotherIsNearer()
	{
		tracker.commit(talkToWillOrAnne, ANNE, 0);
		handled.add(ANNE);
		handled.add(WILL);

		assertEquals(Integer.valueOf(ANNE), select(talkToWillOrAnne, Arrays.asList(ANNE, WILL), 1_000));
	}

	@Test
	public void releaseAfterDialogueRestoresNextUnhandledNpc()
	{
		tracker.commit(talkToWillOrAnne, ANNE, 0);
		handled.add(ANNE);
		tracker.release();

		assertEquals(Integer.valueOf(WILL), select(talkToWillOrAnne, Arrays.asList(ANNE, WILL), 1_000));
	}

	@Test
	public void commitDoesNotCarryOverToAnotherStep()
	{
		tracker.commit(talkToWillOrAnne, ANNE, 0);
		handled.add(ANNE);

		assertEquals(Integer.valueOf(WILL), select(mock(QuestStep.class), Arrays.asList(ANNE, WILL), 1_000));
	}

	@Test
	public void fallsBackWhenCommittedNpcIsGone()
	{
		tracker.commit(talkToWillOrAnne, ANNE, 0);
		handled.add(ANNE);

		assertEquals(Integer.valueOf(WILL), select(talkToWillOrAnne, Collections.singletonList(WILL), 1_000));
	}

	@Test
	public void commitExpiresWhenNoDialogueFollows()
	{
		tracker.commit(talkToWillOrAnne, ANNE, 0);
		handled.add(ANNE);

		assertEquals(Integer.valueOf(WILL), select(talkToWillOrAnne, Arrays.asList(ANNE, WILL), MultiNpcTargetTracker.COMMIT_TIMEOUT_MS));
	}

	@Test
	public void reclickingSameNpcDoesNotExtendCommit()
	{
		tracker.commit(talkToWillOrAnne, ANNE, 0);
		tracker.commit(talkToWillOrAnne, ANNE, MultiNpcTargetTracker.COMMIT_TIMEOUT_MS - 1);
		handled.add(ANNE);

		assertEquals(Integer.valueOf(WILL), select(talkToWillOrAnne, Arrays.asList(ANNE, WILL), MultiNpcTargetTracker.COMMIT_TIMEOUT_MS));
	}

	@Test
	public void allHandledWithoutCommitPicksNearest()
	{
		handled.add(ANNE);
		handled.add(WILL);

		assertEquals(Integer.valueOf(WILL), select(talkToWillOrAnne, Arrays.asList(ANNE, WILL), 0));
	}

	@Test
	public void noNpcsSelectsNothing()
	{
		tracker.commit(talkToWillOrAnne, ANNE, 0);

		assertNull(select(talkToWillOrAnne, Collections.emptyList(), 1_000));
	}
}
