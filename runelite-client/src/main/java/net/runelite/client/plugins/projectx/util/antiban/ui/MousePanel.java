package net.runelite.client.plugins.projectx.util.antiban.ui;

import net.runelite.client.plugins.projectx.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.projectx.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.projectx.util.antiban.enums.ActivityIntensity;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

/**
 * Mouse movement, and the intensity that drives its speed.
 *
 * <p>Each percentage is a caption with its value on the right and a plain slider
 * under it. The previous version put the number inside the caption text and
 * painted a tick scale under every slider, so the numbers moved as they changed
 * and the scale was unreadable at sidebar width.
 *
 * <p>The intensity slider used to call the same setter scripts use, which turns dynamic intensity off. Its
 * listener also fired whenever the refresh moved it, so the first time the intensity changed by itself --
 * the one thing dynamic intensity is for -- the panel switched dynamic intensity off. It now only reacts to
 * the user, and a hand-picked intensity is saved with the other settings.
 */
public class MousePanel extends JPanel
{
    /** The slider positions map to this fixed order of intensities. */
    private static final ActivityIntensity[] INTENSITIES = {
        ActivityIntensity.VERY_LOW,
        ActivityIntensity.LOW,
        ActivityIntensity.MODERATE,
        ActivityIntensity.HIGH,
        ActivityIntensity.EXTREME
    };

    private static final int DEFAULT_INTENSITY_INDEX = 2;

    private final JCheckBox useNaturalMouse = AntibanUi.toggle("Natural mouse",
        "Simulate human-like mouse movements");

    private final JCheckBox simulateMistakes = AntibanUi.toggle("Simulate mistakes",
        "Simulate mistakes in mouse movements");

    private final JCheckBox moveMouseOffScreen = AntibanUi.toggle("Move off screen when idle",
        "Move the mouse off screen if activity cooldown is active");

    private final JLabel offScreenValue = new JLabel();
    private final JSlider moveMouseOffScreenChance =
        AntibanUi.slider(0, 100, (int) (Rs2AntibanSettings.moveMouseOffScreenChance * 100));

    private final JCheckBox moveMouseRandomly = AntibanUi.toggle("Move randomly when idle",
        "Move the mouse randomly when activity cooldown is active");

    private final JLabel randomlyValue = new JLabel();
    private final JSlider moveMouseRandomlyChance =
        AntibanUi.slider(0, 100, (int) (Rs2AntibanSettings.moveMouseRandomlyChance * 100));

    private final JLabel intensityValue = new JLabel();
    private final JSlider mouseSpeedSlider = AntibanUi.slider(0, INTENSITIES.length - 1, DEFAULT_INTENSITY_INDEX);

    public MousePanel()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(AntibanUi.BACKGROUND);

        JPanel movement = AntibanUi.card("Movement");
        movement.add(useNaturalMouse);
        movement.add(simulateMistakes);

        JPanel idle = AntibanUi.card("While on cooldown");
        idle.add(moveMouseOffScreen);
        idle.add(AntibanUi.readout(new JLabel("Off screen chance"), offScreenValue));
        idle.add(moveMouseOffScreenChance);
        idle.add(AntibanUi.gap(4));
        idle.add(moveMouseRandomly);
        idle.add(AntibanUi.readout(new JLabel("Random move chance"), randomlyValue));
        idle.add(moveMouseRandomlyChance);

        JPanel speed = AntibanUi.card("Speed");
        speed.add(AntibanUi.readout(new JLabel("Activity intensity"), intensityValue));
        speed.add(mouseSpeedSlider);
        speed.add(AntibanUi.hint("Higher intensity means faster, less careful movement. Picking one "
            + "by hand turns off Dynamic intensity on the Activity tab."));

        mouseSpeedSlider.setToolTipText("Controls the overall mouse speed/intensity");
        mouseSpeedSlider.setSnapToTicks(true);

        add(movement);
        add(idle);
        add(speed);

        setupActionListeners();
        updateValues();
    }

    private void setupActionListeners()
    {
        AntibanUi.onUserToggle(useNaturalMouse, on -> Rs2AntibanSettings.naturalMouse = on);
        AntibanUi.onUserToggle(simulateMistakes, on -> Rs2AntibanSettings.simulateMistakes = on);
        AntibanUi.onUserToggle(moveMouseOffScreen, on -> Rs2AntibanSettings.moveMouseOffScreen = on);
        AntibanUi.onUserToggle(moveMouseRandomly, on -> Rs2AntibanSettings.moveMouseRandomly = on);

        AntibanUi.onUserSlide(moveMouseOffScreenChance,
            value -> offScreenValue.setText(value + "%"),
            value -> Rs2AntibanSettings.userChange(() -> Rs2AntibanSettings.moveMouseOffScreenChance = value / 100.0));
        AntibanUi.onUserSlide(moveMouseRandomlyChance,
            value -> randomlyValue.setText(value + "%"),
            value -> Rs2AntibanSettings.userChange(() -> Rs2AntibanSettings.moveMouseRandomlyChance = value / 100.0));
        AntibanUi.onUserSlide(mouseSpeedSlider,
            value -> intensityValue.setText(intensityAt(value).getName()),
            value ->
            {
                ActivityIntensity intensity = intensityAt(value);
                Rs2AntibanSettings.userChange(() ->
                {
                    Rs2AntibanSettings.preferredIntensity = intensity;
                    Rs2AntibanSettings.dynamicIntensity = false;
                    Rs2Antiban.updateActivityIntensity(intensity);
                });
            });
    }

    public void updateValues()
    {
        useNaturalMouse.setSelected(Rs2AntibanSettings.naturalMouse);
        simulateMistakes.setSelected(Rs2AntibanSettings.simulateMistakes);

        moveMouseOffScreen.setSelected(Rs2AntibanSettings.moveMouseOffScreen);
        AntibanUi.setQuietly(moveMouseOffScreenChance, (int) Math.round(Rs2AntibanSettings.moveMouseOffScreenChance * 100));
        offScreenValue.setText(moveMouseOffScreenChance.getValue() + "%");

        moveMouseRandomly.setSelected(Rs2AntibanSettings.moveMouseRandomly);
        AntibanUi.setQuietly(moveMouseRandomlyChance, (int) Math.round(Rs2AntibanSettings.moveMouseRandomlyChance * 100));
        randomlyValue.setText(moveMouseRandomlyChance.getValue() + "%");

        ActivityIntensity current = Rs2Antiban.getActivityIntensity();
        AntibanUi.setQuietly(mouseSpeedSlider, indexOf(current));
        if (!mouseSpeedSlider.getValueIsAdjusting())
        {
            intensityValue.setText(current == null ? AntibanUi.NO_VALUE : current.getName());
        }
    }

    private static ActivityIntensity intensityAt(int index)
    {
        return index >= 0 && index < INTENSITIES.length ? INTENSITIES[index] : INTENSITIES[DEFAULT_INTENSITY_INDEX];
    }

    private static int indexOf(ActivityIntensity intensity)
    {
        for (int i = 0; i < INTENSITIES.length; i++)
        {
            if (INTENSITIES[i] == intensity)
            {
                return i;
            }
        }
        return DEFAULT_INTENSITY_INDEX;
    }
}
