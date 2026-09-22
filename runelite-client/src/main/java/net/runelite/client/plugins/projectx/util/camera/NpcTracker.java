package net.runelite.client.plugins.projectx.util.camera;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.util.npc.Rs2Npc;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NpcTracker {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1); // Single-threaded scheduler
    private ScheduledFuture<?> trackingTask; // Future to manage the scheduled task

    // The original method to track the actor
    private static void trackNpc(int npcId) {
        if (!ProjectX.isLoggedIn()) {
            return;
        }
        Actor actor = Rs2Npc.getNpc(npcId); // Get the actor
        if (actor == null) {
            return; // Actor not found, do nothing
        }
        int yaw = Rs2Camera.calculateCameraYaw(Rs2Camera.angleToTile(actor));
        int clientYaw = Rs2Camera.toClientAngleUnits(yaw);
        ProjectX.getClientThread().invokeLater(() -> ProjectX.getClient().setCameraYawTarget(clientYaw));
    }

    /**
     * Method to start tracking the NPC
     *
     * @param npcId the ID of the NPC to track
     */
    public void startTracking(int npcId) {
        if (trackingTask != null && !trackingTask.isCancelled()) {
            ProjectX.log("Already tracking an NPC");
            return; // Already tracking, do nothing
        }

        // Schedule the trackActor method to run every 50 milliseconds
        trackingTask = scheduler.scheduleAtFixedRate(() -> trackNpc(npcId), 0, 200, TimeUnit.MILLISECONDS);
        ProjectX.log("Started tracking NPC with ID: " + npcId);
    }

    /**
     * Method to stop tracking the NPC
     */
    public void stopTracking() {
        if (trackingTask != null) {
            trackingTask.cancel(true); // Cancel the scheduled task
            trackingTask = null;
            ProjectX.log("Stopped tracking NPC");
        }
    }

    /**
     * Method to check if a NPC is being tracked
     *
     * @return true if a NPC is being tracked, false otherwise
     */
    public boolean isTracking() {
        return trackingTask != null;
    }
}
