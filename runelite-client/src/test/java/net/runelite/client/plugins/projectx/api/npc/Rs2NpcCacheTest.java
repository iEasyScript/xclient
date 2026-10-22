package net.runelite.client.plugins.projectx.api.npc;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import net.runelite.api.Client;
import net.runelite.api.IndexedObjectSet;
import net.runelite.api.NPC;
import net.runelite.api.WorldView;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.api.npc.models.Rs2NpcModel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class Rs2NpcCacheTest
{
	private static final int WORLD_VIEW_ID = 4244;

	private final AtomicBoolean onClientThread = new AtomicBoolean();
	private final AtomicInteger tick = new AtomicInteger(5);
	private final AtomicInteger iterations = new AtomicInteger();

	private Client client;
	private ClientThread clientThread;
	private NPC first;
	private NPC second;
	private Rs2NpcCache cache;

	@Before
	public void setUp()
	{
		client = mock(Client.class);
		clientThread = mock(ClientThread.class);
		when(clientThread.runOnClientThreadOptional(any())).thenAnswer(invocation ->
		{
			Callable<?> callable = invocation.getArgument(0);
			onClientThread.set(true);
			try
			{
				return Optional.ofNullable(callable.call());
			}
			finally
			{
				onClientThread.set(false);
			}
		});

		first = mock(NPC.class);
		second = mock(NPC.class);
		IndexedObjectSet<NPC> npcs = new IndexedObjectSet<>()
		{
			@Override
			public NPC byIndex(int index)
			{
				return null;
			}

			@Override
			public Iterator<NPC> iterator()
			{
				iterations.incrementAndGet();
				Iterator<NPC> delegate = Arrays.asList(first, null, second).iterator();
				return new Iterator<>()
				{
					@Override
					public boolean hasNext()
					{
						return guarded(delegate.hasNext());
					}

					@Override
					public NPC next()
					{
						return guarded(delegate.next());
					}
				};
			}
		};

		WorldView worldView = mock(WorldView.class);
		doReturn(npcs).when(worldView).npcs();
		when(client.getTickCount()).thenAnswer(invocation -> guarded(tick.get()));
		when(client.getWorldView(WORLD_VIEW_ID)).thenAnswer(invocation -> guarded(worldView));

		ProjectX.getWorldViewIds().add(WORLD_VIEW_ID);
		cache = new Rs2NpcCache(client, clientThread);
	}

	@After
	public void tearDown()
	{
		ProjectX.getWorldViewIds().remove(WORLD_VIEW_ID);
	}

	private <T> T guarded(T value)
	{
		assertTrue("client state must only be read inside the client-thread task", onClientThread.get());
		return value;
	}

	@Test
	public void offClientThreadRefreshIteratesNpcsOnlyInsideClientThreadTask()
	{
		List<Rs2NpcModel> npcs = cache.getStream().collect(Collectors.toList());

		assertEquals(2, npcs.size());
		verify(clientThread, times(1)).runOnClientThreadOptional(any());
	}

	@Test
	public void sameTickReusesSnapshotAndNextTickRescans()
	{
		assertEquals(2, cache.getStream().count());
		assertEquals(2, cache.getStream().count());
		assertEquals(1, iterations.get());

		tick.incrementAndGet();
		assertEquals(2, cache.getStream().count());
		assertEquals(2, iterations.get());
	}

	@Test
	public void clientThreadFailureReturnsEmptyStreamWithoutReadingClient()
	{
		doReturn(Optional.empty()).when(clientThread).runOnClientThreadOptional(any());

		assertEquals(0, cache.getStream().count());
		verifyNoInteractions(client);
	}
}
