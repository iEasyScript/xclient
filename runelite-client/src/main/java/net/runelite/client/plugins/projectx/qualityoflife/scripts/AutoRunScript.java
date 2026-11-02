package net.runelite.client.plugins.projectx.qualityoflife.scripts;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.Script;
import net.runelite.client.plugins.projectx.qualityoflife.QoLConfig;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class AutoRunScript extends Script {

    @Inject
    public ConfigManager configManager;


    public boolean run(QoLConfig config) {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!ProjectX.isLoggedIn()) return;
                if (!super.run()) {
                    return;
                }
                if(ProjectX.useStaminaPotsIfNeeded != config.autoStamina()) {
                    configManager.setConfiguration("QoL", "autoStamina", ProjectX.useStaminaPotsIfNeeded);
                }
                if(ProjectX.runEnergyThreshold/100 != config.staminaThreshold()) {
                    configManager.setConfiguration("QoL", "staminaThreshold", ProjectX.runEnergyThreshold/100);
                }
            } catch (Exception ex) {
                ProjectX.logStackTrace(this.getClass().getSimpleName(), ex);
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        return true;
    }

    public void shutdown() {
        super.shutdown();
    }
}
