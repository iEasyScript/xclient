package net.runelite.client.plugins.projectx;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.projectx.shortestpath.ShortestPathPlugin;
import net.runelite.client.plugins.projectx.util.Global;
import net.runelite.client.plugins.projectx.agentserver.handler.ScriptHeartbeatRegistry;
import net.runelite.client.plugins.projectx.util.antiban.AntibanPlugin;
import net.runelite.client.plugins.projectx.util.antiban.SessionFatigue;
import net.runelite.client.plugins.projectx.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;
import net.runelite.client.plugins.projectx.util.walker.Rs2Walker;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base class for ProjectX automation scripts.
 * Provides scheduling helpers, guards against client-thread misuse, and common shutdown/reset logic.
 */
@Slf4j
public abstract class Script extends Global implements IScript {
    protected ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(10,
        new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread t = new Thread(r);
                t.setName(Script.this.getClass().getSimpleName() + "-" + threadNumber.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        });
    protected ScheduledFuture<?> scheduledFuture;
    protected ScheduledFuture<?> mainScheduledFuture;

    /**
     * Indicates whether the main scheduled script loop is still active.
     */
    public boolean isRunning() {
        return mainScheduledFuture != null && !mainScheduledFuture.isDone();
    }

    @Getter
    protected static WorldPoint initialPlayerLocation;

    /**
     * Cancel scheduled tasks, clear shared state, and reset helpers.
     * Safe to call multiple times; no-ops if already shut down.
     */
    public void shutdown() {
        ScriptHeartbeatRegistry.remove(this.getClass().getName());
        if (mainScheduledFuture != null && !mainScheduledFuture.isDone()) {
            mainScheduledFuture.cancel(true);
            // Lets the session tracker tell a script that stopped by itself from one being switched off.
            net.runelite.client.plugins.projectx.externalplugins.ProjectXSessionTracker.onScriptShutdown(this, this::isRunning);
            AntibanPlugin.scriptStopped();
            ShortestPathPlugin.exit();
            if (ProjectX.getClientThread().scheduledFuture != null)
                ProjectX.getClientThread().scheduledFuture.cancel(true);
            initialPlayerLocation = null;
            ProjectX.pauseAllScripts.set(false);
            Rs2Walker.disableTeleports = false;
            ProjectX.getSpecialAttackConfigs().reset();
        }
        if (scheduledFuture != null && !scheduledFuture.isDone()) {
            scheduledFuture.cancel(true);
        }
    }

    /**
     * Default pre-loop guard invoked by script schedulers.
     * Returns {@code false} to pause a loop when a blocking event is executing, scripts are paused,
     * tutorial island is incomplete, or the current thread is interrupted.
     */
    public boolean run() {
        ScriptHeartbeatRegistry.recordHeartbeat(this.getClass().getName());

        if (ProjectX.isLoggedIn() && !SessionFatigue.isActive()) {
            SessionFatigue.startSession();
        }

        if (ProjectX.isLoggedIn() && !Rs2Player.hasCompletedTutorialIsland())
            return true;

        if (Rs2Player.hasCompletedTutorialIsland() && ProjectX.getBlockingEventManager().shouldBlockAndProcess()) {
            // A blocking event was found & is executing
            return false;
        }
        if (ProjectX.pauseAllScripts.get())
            return false;
        if (Thread.currentThread().isInterrupted())
            return false;

        if (ProjectX.isLoggedIn()) {
            boolean hasRunEnergy = ProjectX.getClientThread().runOnClientThreadOptional(() -> ProjectX.getClient().getEnergy()).orElse(0) > ProjectX.runEnergyThreshold;
            if (ProjectX.enableAutoRunOn && hasRunEnergy)
                Rs2Player.toggleRunEnergy(true);
            if (!hasRunEnergy && ProjectX.useStaminaPotsIfNeeded && Rs2Player.isMoving()) {
                Rs2Inventory.useRestoreEnergyItem();
            }
            ProjectX.getConfigManager().setConfiguration(ProjectXConfig.configGroup, ProjectXConfig.keyEnableAutoRunOn, ProjectX.enableAutoRunOn);
            ProjectX.getConfigManager().setConfiguration(ProjectXConfig.configGroup, ProjectXConfig.keyUseStaminaPotsIfNeeded, ProjectX.useStaminaPotsIfNeeded);
        }
        return true;
    }
}
