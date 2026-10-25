package net.runelite.client.plugins.projectx.questhelper.runeliteobjects;

import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.plugins.projectx.questhelper.runeliteobjects.extendedruneliteobjects.FakeNpc;
import net.runelite.client.plugins.projectx.questhelper.runeliteobjects.extendedruneliteobjects.RuneliteObjectManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GlobalFakeObjectsTest
{
	private static final int QUEST_CAPE_MALE = 18946;
	private static final int WISE_OLD_MAN_MODEL = 1234;

	private Client client;
	private RuneliteObjectManager runeliteObjectManager;
	private ModelData modelData;

	@Before
	public void setUp()
	{
		GlobalFakeObjects.setInitialized(false);

		client = mock(Client.class);
		Player player = mock(Player.class);
		when(player.getWorldLocation()).thenReturn(new WorldPoint(3200, 3200, 0));
		when(client.getLocalPlayer()).thenReturn(player);

		NPCComposition wiseOldMan = mock(NPCComposition.class);
		when(wiseOldMan.getModels()).thenReturn(new int[]{WISE_OLD_MAN_MODEL});
		when(client.getNpcDefinition(NpcID.WISE_OLD_MAN)).thenReturn(wiseOldMan);

		modelData = mock(ModelData.class);
		Model model = mock(Model.class);
		when(modelData.cloneColors()).thenReturn(modelData);
		when(modelData.recolor(anyShort(), anyShort())).thenReturn(modelData);
		when(modelData.light()).thenReturn(model);
		when(modelData.light(anyInt(), anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(model);
		when(client.loadModelData(anyInt())).thenReturn(modelData);
		when(client.mergeModels(any(ModelData[].class))).thenReturn(modelData);

		runeliteObjectManager = mock(RuneliteObjectManager.class);
		when(runeliteObjectManager.createFakeNpc(anyString(), any(Model.class), any(WorldPoint.class), anyInt()))
			.thenReturn(mock(FakeNpc.class));
	}

	@After
	public void tearDown()
	{
		GlobalFakeObjects.setInitialized(false);
	}

	@Test
	public void unloadedCapeModelCreatesNoCheerers()
	{
		when(client.loadModelData(QUEST_CAPE_MALE)).thenReturn(null);

		assertFalse(Cheerer.createCheerers(runeliteObjectManager, client, null));

		verify(runeliteObjectManager, never()).createFakeNpc(anyString(), any(Model.class), any(WorldPoint.class), anyInt());
	}

	@Test
	public void unloadedBodyModelCreatesNoCheerers()
	{
		when(client.loadModelData(WISE_OLD_MAN_MODEL)).thenReturn(null);

		assertFalse(Cheerer.createCheerers(runeliteObjectManager, client, null));

		verify(runeliteObjectManager, never()).createFakeNpc(anyString(), any(Model.class), any(WorldPoint.class), anyInt());
	}

	@Test
	public void loadedModelsCreateBothCheerers()
	{
		assertTrue(Cheerer.createCheerers(runeliteObjectManager, client, null));

		verify(runeliteObjectManager, times(2)).createFakeNpc(eq("global"), any(Model.class), any(WorldPoint.class), anyInt());
	}

	@Test
	public void createNpcsRetriesAfterModelsWereNotLoaded()
	{
		when(client.loadModelData(QUEST_CAPE_MALE)).thenReturn(null);
		GlobalFakeObjects.createNpcs(client, runeliteObjectManager, null, null);
		verify(runeliteObjectManager, never()).createFakeNpc(anyString(), any(Model.class), any(WorldPoint.class), anyInt());

		when(client.loadModelData(QUEST_CAPE_MALE)).thenReturn(modelData);
		GlobalFakeObjects.createNpcs(client, runeliteObjectManager, null, null);
		GlobalFakeObjects.createNpcs(client, runeliteObjectManager, null, null);

		verify(runeliteObjectManager, times(2)).createFakeNpc(eq("global"), any(Model.class), any(WorldPoint.class), anyInt());
	}

	@Test
	public void activateCheererWithoutCheerersDoesNothing()
	{
		when(client.getLocalPlayer()).thenReturn(null);
		Cheerer.createCheerers(runeliteObjectManager, client, null);

		Cheerer.activateCheerer(client, null);
	}
}
