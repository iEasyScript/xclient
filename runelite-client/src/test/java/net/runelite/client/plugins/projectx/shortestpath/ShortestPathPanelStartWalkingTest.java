package net.runelite.client.plugins.projectx.shortestpath;

import java.lang.reflect.Field;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.projectx.util.walker.enums.SlayerMasters;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ShortestPathPanelStartWalkingTest
{
	private ShortestPathPanel panel;
	private ShortestPathScript script;

	@Before
	public void setUp() throws Exception
	{
		panel = mock(ShortestPathPanel.class, Mockito.CALLS_REAL_METHODS);
		ShortestPathPlugin plugin = mock(ShortestPathPlugin.class);
		script = mock(ShortestPathScript.class);
		when(plugin.getShortestPathScript()).thenReturn(script);
		setField("plugin", plugin);
	}

	@Test
	public void startWalkingIgnoresNullTarget()
	{
		panel.startWalking(null);

		verify(script, never()).setTriggerWalker(any());
	}

	@Test
	public void startWalkingTriggersWalkerForTarget()
	{
		WorldPoint target = new WorldPoint(3200, 3200, 0);

		panel.startWalking(target);

		verify(script).setTriggerWalker(target);
	}

	@Test
	public void slayerMasterWithoutLocationDoesNotTriggerWalker() throws Exception
	{
		JComboBox<SlayerMasters> comboBox = new JComboBox<>(SlayerMasters.values());
		comboBox.setSelectedItem(SlayerMasters.NONE);
		setField("slayerMasterComboBox", comboBox);

		assertNull(panel.getSlayerMasterTarget());
		panel.startWalking(panel.getSlayerMasterTarget());

		verify(script, never()).setTriggerWalker(any());
	}

	@Test
	public void invalidCustomLocationDoesNotTriggerWalker() throws Exception
	{
		setField("xField", new JTextField("abc"));
		setField("yField", new JTextField("3200"));
		setField("zField", new JTextField("0"));

		assertNull(panel.getCustomLocation());
		panel.startWalking(panel.getCustomLocation());

		verify(script, never()).setTriggerWalker(any());
	}

	private void setField(String name, Object value) throws Exception
	{
		Field field = ShortestPathPanel.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(panel, value);
	}
}
