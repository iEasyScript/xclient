package net.runelite.client.plugins.grounditems;

import com.google.common.collect.Table;
import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.runelite.api.coords.WorldPoint;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class GroundItemsPluginCollectedItemsTest
{
	private static final int ITEMS = 200;

	private Table<WorldPoint, Integer, GroundItem> collected;
	private ExecutorService executor;

	@Before
	@SuppressWarnings("unchecked")
	public void setUp() throws Exception
	{
		Field field = GroundItemsPlugin.class.getDeclaredField("collectedGroundItems");
		field.setAccessible(true);
		collected = (Table<WorldPoint, Integer, GroundItem>) field.get(null);
		collected.clear();
		executor = Executors.newSingleThreadExecutor();
	}

	@After
	public void tearDown()
	{
		executor.shutdownNow();
		collected.clear();
	}

	@Test
	public void copyIsSafeWhileItemsSpawnAndDespawn() throws Exception
	{
		for (int i = 0; i < ITEMS; i++)
		{
			put(i);
		}

		AtomicBoolean running = new AtomicBoolean(true);
		Future<?> writer = executor.submit(() ->
		{
			int i = ITEMS;
			while (running.get())
			{
				put(i);
				collected.remove(new WorldPoint(3200 + (i - ITEMS) % ITEMS, 3200, 0), i - ITEMS);
				i++;
			}
		});

		try
		{
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
			while (System.nanoTime() < deadline)
			{
				assertTrue(GroundItemsPlugin.getCollectedGroundItems().size() >= ITEMS);
			}
		}
		finally
		{
			running.set(false);
		}
		writer.get(5, TimeUnit.SECONDS);
	}

	private void put(int id)
	{
		WorldPoint point = new WorldPoint(3200 + id % ITEMS, 3200, 0);
		collected.put(point, id, GroundItem.builder().id(id).location(point).build());
	}
}
