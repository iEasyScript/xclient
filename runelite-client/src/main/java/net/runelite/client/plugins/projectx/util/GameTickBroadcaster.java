package net.runelite.client.plugins.projectx.util;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Singleton
public final class GameTickBroadcaster {

    private static final long DEFAULT_TIMEOUT_MS = 1200;

    private volatile CountDownLatch currentLatch = new CountDownLatch(1);

    @Inject
    public GameTickBroadcaster(EventBus eventBus) {
        eventBus.register(this);
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        CountDownLatch old = currentLatch;
        currentLatch = new CountDownLatch(1);
        old.countDown();
    }

    public boolean awaitNextTick() throws InterruptedException {
        return awaitNextTick(DEFAULT_TIMEOUT_MS);
    }

    public boolean awaitNextTick(long timeoutMs) throws InterruptedException {
        return currentLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
    }
}
