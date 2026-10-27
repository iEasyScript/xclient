package net.runelite.client.plugins.projectx.bot;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.WallObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.api.AbstractEntityQueryable;
import net.runelite.client.plugins.projectx.api.IEntity;
import net.runelite.client.plugins.projectx.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.projectx.api.tileitem.models.Rs2TileItemModel;
import net.runelite.client.plugins.projectx.api.tileobject.Rs2TileObjectCache;
import net.runelite.client.plugins.projectx.api.tileobject.Rs2TileObjectQueryable;
import net.runelite.client.plugins.projectx.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.projectx.util.Rs2InventorySetup;
import net.runelite.client.plugins.projectx.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.projectx.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.projectx.util.bank.Rs2Bank;
import net.runelite.client.plugins.projectx.util.camera.Rs2Camera;
import net.runelite.client.plugins.projectx.util.combat.Rs2Combat;
import net.runelite.client.plugins.projectx.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.projectx.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.projectx.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.projectx.util.grandexchange.Rs2GrandExchange;
import net.runelite.client.plugins.projectx.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.projectx.util.grounditem.Rs2LootEngine;
import net.runelite.client.plugins.projectx.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.projectx.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.projectx.util.magic.Rs2CombatSpells;
import net.runelite.client.plugins.projectx.util.magic.Rs2Magic;
import net.runelite.client.plugins.projectx.util.magic.Rs2Spellbook;
import net.runelite.client.plugins.projectx.util.magic.Rs2Spells;
import net.runelite.client.plugins.projectx.util.magic.Rs2Staff;
import net.runelite.client.plugins.projectx.util.magic.Rs2Tome;
import net.runelite.client.plugins.projectx.util.magic.thralls.Rs2Thrall;
import net.runelite.client.plugins.projectx.util.math.Rs2Random;
import net.runelite.client.plugins.projectx.util.misc.Rs2Food;
import net.runelite.client.plugins.projectx.util.misc.Rs2Potion;
import net.runelite.client.plugins.projectx.util.npc.Rs2Npc;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;
import net.runelite.client.plugins.projectx.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.projectx.util.prayer.Rs2PrayerEnum;
import net.runelite.client.plugins.projectx.util.item.Rs2EnsouledHead;
import net.runelite.client.plugins.projectx.util.shop.Rs2Shop;
import net.runelite.client.plugins.projectx.util.tabs.Rs2Tab;
import net.runelite.client.plugins.projectx.util.text.Rs2TextSanitizer;
import net.runelite.client.plugins.projectx.util.tile.Rs2Tile;
import net.runelite.client.plugins.projectx.util.walker.Rs2PathApi;
import net.runelite.client.plugins.projectx.util.walker.Rs2Walker;
import net.runelite.client.plugins.projectx.util.widget.Rs2Widget;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The short names in {@code bot} must be the old classes under a new name: same methods, same
 * constants, same cache, same objects.
 */
@SuppressWarnings({"deprecation", "removal"})
public class BotApiTest
{
    private static final int WORLD_VIEW_ID = 7731;

    /** new name -> old class, for the helpers that are plain subclasses. */
    private static final Map<Class<?>, Class<?>> HELPERS = Map.ofEntries(
        Map.entry(Inventory.class, Rs2Inventory.class),
        Map.entry(Player.class, Rs2Player.class),
        Map.entry(Bank.class, Rs2Bank.class),
        Map.entry(Walker.class, Rs2Walker.class),
        Map.entry(Widgets.class, Rs2Widget.class),
        Map.entry(Dialogue.class, Rs2Dialogue.class),
        Map.entry(Equipment.class, Rs2Equipment.class),
        Map.entry(Rand.class, Rs2Random.class),
        Map.entry(AntibanSettings.class, Rs2AntibanSettings.class),
        Map.entry(Antiban.class, Rs2Antiban.class),
        Map.entry(GrandExchange.class, Rs2GrandExchange.class),
        Map.entry(Prayers.class, Rs2Prayer.class),
        Map.entry(Objects.class, Rs2GameObject.class),
        Map.entry(Keyboard.class, Rs2Keyboard.class),
        Map.entry(Magic.class, Rs2Magic.class),
        Map.entry(Combat.class, Rs2Combat.class),
        Map.entry(Camera.class, Rs2Camera.class),
        Map.entry(Tabs.class, Rs2Tab.class),
        Map.entry(Tiles.class, Rs2Tile.class),
        Map.entry(GroundItems.class, Rs2GroundItem.class),
        Map.entry(Npcs.class, Rs2Npc.class),
        Map.entry(Shop.class, Rs2Shop.class),
        Map.entry(Potions.class, Rs2Potion.class),
        Map.entry(LootEngine.class, Rs2LootEngine.class),
        Map.entry(PathApi.class, Rs2PathApi.class),
        Map.entry(TextSanitizer.class, Rs2TextSanitizer.class),
        Map.entry(Loadout.class, Rs2InventorySetup.class)
    );

    private Object previousTileObjectCache;

    @Before
    public void rememberCache() throws Exception
    {
        Field field = ProjectX.class.getDeclaredField("rs2TileObjectCache");
        field.setAccessible(true);
        previousTileObjectCache = field.get(null);
    }

    @After
    public void restoreCache() throws Exception
    {
        swapStatic("rs2TileObjectCache", previousTileObjectCache);
        ProjectX.getWorldViewIds().remove(WORLD_VIEW_ID);
    }

    @Test
    public void everyHelperIsTheOldClassUnderANewName() throws Exception
    {
        for (Map.Entry<Class<?>, Class<?>> e : HELPERS.entrySet())
        {
            Class<?> neu = e.getKey();
            Class<?> old = e.getValue();
            assertSame(neu + " must extend " + old, old, neu.getSuperclass());
            for (Method m : old.getDeclaredMethods())
            {
                if (!Modifier.isPublic(m.getModifiers()) || !Modifier.isStatic(m.getModifiers()))
                {
                    continue;
                }
                Method viaNewName = neu.getMethod(m.getName(), m.getParameterTypes());
                assertEquals(neu.getSimpleName() + "." + m.getName() + " must be " + old.getSimpleName() + "'s method",
                    old, viaNewName.getDeclaringClass());
            }
        }
    }

    @Test
    public void enumHoldersHoldEveryConstantAsTheSameValue() throws Exception
    {
        assertHolds(Prayers.class, Rs2PrayerEnum.class);
        assertHolds(Spells.class, Rs2Spells.class);
        assertHolds(CombatSpells.class, Rs2CombatSpells.class);
        assertHolds(Foods.class, Rs2Food.class);
        assertHolds(Spellbooks.class, Rs2Spellbook.class);
        assertHolds(Staffs.class, Rs2Staff.class);
        assertHolds(Thralls.class, Rs2Thrall.class);
        assertHolds(Tomes.class, Rs2Tome.class);
        assertHolds(EnsouledHeads.class, Rs2EnsouledHead.class);
        assertSame(Rs2PrayerEnum.PROTECT_MELEE, Prayers.PROTECT_MELEE);
        assertEquals(Rs2PrayerEnum.values().length, Prayers.values().length);
    }

    private static <E extends Enum<E>> void assertHolds(Class<?> holder, Class<E> enumType) throws Exception
    {
        for (E constant : enumType.getEnumConstants())
        {
            Field f = holder.getField(constant.name());
            int mod = f.getModifiers();
            assertTrue(holder.getSimpleName() + "." + constant.name() + " must be public static final",
                Modifier.isPublic(mod) && Modifier.isStatic(mod) && Modifier.isFinal(mod));
            assertSame(holder.getSimpleName() + "." + constant.name(), constant, f.get(null));
        }
    }

    @Test
    public void modelInteractClicksWithTheSameAction()
    {
        SceneObject rock = spy(new SceneObject(mock(GameObject.class)));
        doReturn(true).when(rock).click("Mine");
        assertTrue(rock.interact("Mine"));
        verify(rock).click("Mine");

        Npc cow = spy(new Npc(mock(NPC.class)));
        doReturn(true).when(cow).click("Attack");
        assertTrue(cow.interact("Attack"));
        verify(cow).click("Attack");

        doReturn(false).when(cow).click();
        assertFalse(cow.interact());
        verify(cow).click();
    }

    @Test
    public void modelsAreTheOldModelsAndOfKeepsIdentity()
    {
        assertSame(Rs2NpcModel.class, Npc.class.getSuperclass());
        assertSame(Rs2TileObjectModel.class, SceneObject.class.getSuperclass());
        assertSame(Rs2TileItemModel.class, FloorItem.class.getSuperclass());

        NPC npc = mock(NPC.class);
        Npc already = new Npc(npc);
        assertSame(already, Npc.of(already));
        Npc wrapped = Npc.of(new Rs2NpcModel(npc));
        assertSame(npc, wrapped.getNpc());
        assertNull(Npc.of(null));

        GameObject go = mock(GameObject.class);
        SceneObject obj = SceneObject.of(new Rs2TileObjectModel(go));
        assertSame(go, obj.getTileObject());
        assertSame(obj, SceneObject.of(obj));

        Tile tile = mock(Tile.class);
        TileItem item = mock(TileItem.class);
        FloorItem floor = FloorItem.of(new Rs2TileItemModel(tile, item));
        assertSame(item, floor.getTileItem());
        assertSame(tile, floor.getTile());
    }

    @Test
    public void objectsQueryReadsTheTileObjectCacheAndReturnsTheSameObjects() throws Exception
    {
        installTileObjectCache();

        List<SceneObject> viaNewName = Objects.query().list();
        List<Rs2TileObjectModel> viaOldName = ProjectX.getRs2TileObjectCache().query().toList();

        assertEquals(2, viaNewName.size());
        assertEquals(viaOldName.size(), viaNewName.size());
        for (int i = 0; i < viaOldName.size(); i++)
        {
            assertTrue("the cache builds SceneObjects", viaOldName.get(i) instanceof SceneObject);
            assertSame("same per-tick snapshot", viaOldName.get(i), viaNewName.get(i));
        }
        assertTrue(Objects.query() instanceof ObjectQuery);
        assertTrue(ProjectX.getRs2TileObjectCache().query() instanceof Rs2TileObjectQueryable);
        assertEquals(2, Objects.stream().count());
    }

    @Test
    public void nameEqualsIsWithNameAndListIsToList()
    {
        Fake cow = new Fake("Cow");
        Fake chicken = new Fake("Chicken");
        Fake bigCow = new Fake("COW");

        List<Fake> byNameEquals = new FakeQuery(cow, chicken, bigCow).nameEquals("cow").list();
        List<Fake> byWithName = new FakeQuery(cow, chicken, bigCow).withName("cow").toList();
        assertEquals(byWithName, byNameEquals);
        assertEquals(List.of(cow, bigCow), byNameEquals);

        // where(...) accepts a predicate over a supertype, as Rs2NpcModel.matches(...) is for Npc
        java.util.function.Predicate<IEntity> anyNamed = e -> e.getName() != null;
        assertEquals(3, new FakeQuery(cow, chicken, bigCow).where(anyNamed).count());
    }

    @Test
    public void objectsAlsoCoversJavaUtilObjects()
    {
        assertTrue(Objects.equals(null, null));
        assertFalse(Objects.equals("a", null));
        assertEquals(java.util.Objects.hash(1, "x"), Objects.hash(1, "x"));
        assertEquals("d", Objects.requireNonNullElse(null, "d"));
        assertTrue(Objects.isNull(null));
        assertTrue(Objects.nonNull(""));
        assertEquals("null", Objects.toString(null));
        assertNotSame(Objects.class, java.util.Objects.class);
    }

    private void installTileObjectCache() throws Exception
    {
        Client client = mock(Client.class);
        ClientThread clientThread = mock(ClientThread.class);
        when(clientThread.runOnClientThreadOptional(any())).thenAnswer(invocation ->
        {
            Callable<?> callable = invocation.getArgument(0);
            return Optional.ofNullable(callable.call());
        });

        Point tileLocation = new Point(10, 20);
        GameObject rock = mock(GameObject.class);
        when(rock.getSceneMinLocation()).thenReturn(tileLocation);
        WallObject wall = mock(WallObject.class);
        Tile tile = mock(Tile.class);
        when(tile.getSceneLocation()).thenReturn(tileLocation);
        when(tile.getGameObjects()).thenReturn(new GameObject[]{rock});
        when(tile.getWallObject()).thenReturn(wall);

        Scene scene = mock(Scene.class);
        Tile[][][] tiles = new Tile[1][1][1];
        tiles[0][0][0] = tile;
        when(scene.getTiles()).thenReturn(tiles);
        WorldView worldView = mock(WorldView.class);
        when(worldView.getId()).thenReturn(WORLD_VIEW_ID);
        when(worldView.getScene()).thenReturn(scene);
        when(worldView.getPlane()).thenReturn(0);
        when(client.getTickCount()).thenReturn(1);
        when(client.getLocalPlayer()).thenReturn(mock(net.runelite.api.Player.class));
        when(client.getWorldView(WORLD_VIEW_ID)).thenReturn(worldView);

        ProjectX.getWorldViewIds().add(WORLD_VIEW_ID);
        swapStatic("rs2TileObjectCache", new Rs2TileObjectCache(client, clientThread));
    }

    private static Object swapStatic(String name, Object value) throws Exception
    {
        Field field = ProjectX.class.getDeclaredField(name);
        field.setAccessible(true);
        Object previous = field.get(null);
        field.set(null, value);
        return previous;
    }

    private static final class FakeQuery extends AbstractEntityQueryable<FakeQuery, Fake>
    {
        FakeQuery(Fake... entities)
        {
            super();
            this.source = Stream.of(entities);
        }

        @Override
        protected Stream<Fake> initialSource()
        {
            return Stream.empty();
        }
    }

    private static final class Fake implements IEntity
    {
        private final String name;

        Fake(String name)
        {
            this.name = name;
        }

        @Override public int getId() { return 0; }
        @Override public String getName() { return name; }
        @Override public WorldPoint getWorldLocation() { return null; }
        @Override public LocalPoint getLocalLocation() { return null; }
        @Override public WorldView getWorldView() { return null; }
        @Override public boolean click() { return false; }
        @Override public boolean click(String action) { return false; }
    }
}
