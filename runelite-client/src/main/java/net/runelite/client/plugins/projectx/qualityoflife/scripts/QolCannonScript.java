package net.runelite.client.plugins.projectx.qualityoflife.scripts;

import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.Script;
import net.runelite.client.plugins.projectx.qualityoflife.QoLConfig;
import net.runelite.client.plugins.projectx.util.gameobject.Rs2Cannon;

import java.util.concurrent.TimeUnit;

public class QolCannonScript extends Script {
    public boolean run(QoLConfig config) {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!ProjectX.isLoggedIn()) return;
                if (!super.run() || !config.refillCannon()) return;
                if (Rs2Cannon.repair())
                    return;
                Rs2Cannon.refill();
            } catch(Exception ex) {
                ProjectX.logStackTrace(this.getClass().getSimpleName(), ex);
            }
        }, 0, 2000, TimeUnit.MILLISECONDS);
        return true;
    }
}
