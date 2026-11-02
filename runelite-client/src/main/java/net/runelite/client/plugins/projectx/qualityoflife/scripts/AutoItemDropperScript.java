package net.runelite.client.plugins.projectx.qualityoflife.scripts;

import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.Script;
import net.runelite.client.plugins.projectx.qualityoflife.QoLConfig;
import net.runelite.client.plugins.projectx.util.inventory.InteractOrder;
import net.runelite.client.plugins.projectx.util.inventory.Rs2Inventory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AutoItemDropperScript extends Script {

    public boolean run(QoLConfig config) {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!ProjectX.isLoggedIn()) return;
                if (config.autoDrop()) {
                    handleAutoDropItems(config);
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

    private void handleAutoDropItems(QoLConfig config) {
        if (Rs2Inventory.isFull()) {
            List<String> itemsToDrop = Arrays.asList(config.autoDropItems().toLowerCase().trim().split(","));
            InteractOrder dropOrder = InteractOrder.RANDOM;
            if(config.excludeItems()) {
                Rs2Inventory.dropAllExcept(x -> x.getName() != null && !itemsToDrop.contains(x.getName().toLowerCase()));
            }
            else
                Rs2Inventory.dropAll(x -> x.getName() != null && itemsToDrop.contains(x.getName().toLowerCase()), dropOrder);
        }
    }
}
