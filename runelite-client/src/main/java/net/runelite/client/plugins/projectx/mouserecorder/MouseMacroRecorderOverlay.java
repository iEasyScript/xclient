package net.runelite.client.plugins.projectx.mouserecorder;

import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;

import javax.inject.Inject;
import java.awt.Dimension;
import java.awt.Graphics2D;

public class MouseMacroRecorderOverlay extends OverlayPanel
{
	private final MouseMacroRecorderPlugin plugin;

	@Inject
	private MouseMacroRecorderOverlay(MouseMacroRecorderPlugin plugin)
	{
		super(plugin);
		this.plugin = plugin;
		setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.isRecording())
		{
			return null;
		}

		panelComponent.getChildren().clear();
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Mouse")
			.right("Recording")
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Events")
			.right(Integer.toString(plugin.getRecordedEventCount()))
			.build());

		return super.render(graphics);
	}
}
