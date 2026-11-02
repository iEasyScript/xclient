package net.runelite.client.plugins.projectx.qualityoflife.scripts;

import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.Script;
import net.runelite.client.plugins.projectx.qualityoflife.QoLConfig;
import net.runelite.client.plugins.projectx.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.projectx.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class SpecialAttackScript extends Script {

    public boolean run(QoLConfig config) {
        AtomicReference<Rs2NpcModel> npc = new AtomicReference<>();
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!ProjectX.isLoggedIn()) return;
                if (!super.run()) return;
                if (!config.useSpecWeapon()) return;
                if (Rs2Equipment.all("guthan's").count() == 4) return;
                if (Rs2Player.isInteracting()) {
                    var interacting = Rs2Player.getInteracting();
                    if (interacting instanceof net.runelite.api.NPC) {
                        var targetNpc = ProjectX.getRs2NpcCache().query().where(n -> n.getNpc().equals(interacting)).nearest();
                        npc.set(targetNpc);
                    }
                    if (npc.get() != null && ProjectX.getSpecialAttackConfigs().useSpecWeapon()) {
                        npc.get().click("Attack");
                    }
                }
            } catch (Exception ex) {
                ProjectX.logStackTrace(this.getClass().getSimpleName(), ex);
            }
        }, 0, 600, TimeUnit.MILLISECONDS);
        return true;
    }

    public void shutdown() {
        super.shutdown();
    }

}