package net.runelite.client.plugins.projectx.api;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.api.player.models.Rs2PlayerModel;
import net.runelite.client.plugins.projectx.util.coords.SceneSpace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Shared filters and terminals for the queryable caches.
 *
 * <p><b>Coordinates.</b> Every model's {@code getWorldLocation()} (and {@link Rs2PlayerModel}'s)
 * is in the scene's own coordinates, which inside an instance are instance coordinates, so
 * {@link #within(int)}, {@link #nearest()} and friends compare like with like. An anchor passed in
 * by a script is often a template (real-world) point instead, from {@code Rs2Player.getWorldLocation()}
 * or a hard-coded tile; {@link #within(WorldPoint, int)} and {@link #nearest(WorldPoint, int)} map
 * such an anchor onto the scene first (see {@link SceneSpace}).
 */
@Slf4j
public abstract class AbstractEntityQueryable<
        Q extends IEntityQueryable<Q, E>,
        E extends IEntity
        >
        implements IEntityQueryable<Q, E> {

    /**
     * How far, in tiles, {@link #nearestReachable(int)} still picks an entity when reachability
     * cannot be worked out (no player world view, or the check throws). Close enough that the
     * click is almost certainly on the thing the script means.
     */
    static final int REACHABILITY_FALLBACK_DISTANCE = 10;

    protected Stream<E> source;

    protected AbstractEntityQueryable() {
        this.source = initialSource();
    }

    protected abstract Stream<E> initialSource();

    @SuppressWarnings("unchecked")
    @Override
    public Q fromWorldView() {
        var worldView = new Rs2PlayerModel().getWorldView();
        if (worldView == null) {
            this.source = Stream.empty();
            return (Q) this;
        }

        this.source = this.source
                .filter(o -> o.getWorldView() != null && o.getWorldView().getId() == worldView.getId());

        return (Q) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Q where(Predicate<? super E> predicate) {
        source = source.filter(predicate);
        return (Q) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Q within(int distance) {
        WorldPoint playerLoc = new Rs2PlayerModel().getWorldLocation();
        if (playerLoc == null) {
            this.source = Stream.empty();
            return (Q) this;
        }

        this.source = this.source
                .filter(o -> {
                    WorldPoint loc = o.getWorldLocation();
                    return loc != null && loc.distanceTo(playerLoc) <= distance;
                });

        return (Q) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Q within(WorldPoint anchor, int distance) {
        if (anchor == null) {
            this.source = Stream.empty();
            return (Q) this;
        }

        WorldPoint sceneAnchor = toSceneSpace(anchor);
        this.source = this.source
                .filter(o -> {
                    WorldPoint loc = o.getWorldLocation();
                    return loc != null && loc.distanceTo(sceneAnchor) <= distance;
                });

        return (Q) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Q withName(String name) {
        if (name == null) {
            this.source = Stream.empty();
            return (Q) this;
        }

        this.source = this.source.filter(x -> {
            String n = x.getName();
            return n != null && n.equalsIgnoreCase(name);
        });

        return (Q) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Q withNameContains(String substring) {
        if (substring == null || substring.isEmpty()) {
            this.source = Stream.empty();
            return (Q) this;
        }

        String needle = substring.toLowerCase();
        this.source = this.source.filter(x -> {
            String n = x.getName();
            return n != null && n.toLowerCase().contains(needle);
        });

        return (Q) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Q withNames(String... names) {
        if (names == null || names.length == 0) {
            this.source = Stream.empty();
            return (Q) this;
        }

        this.source = this.source.filter(x -> {
            String n = x.getName();
            if (n == null) return false;
            return Arrays.stream(names).anyMatch(n::equalsIgnoreCase);
        });

        return (Q) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Q withId(int id) {
        this.source = this.source.filter(x -> x.getId() == id);
        return (Q) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Q withIds(int... ids) {
        if (ids == null || ids.length == 0) {
            this.source = Stream.empty();
            return (Q) this;
        }

        this.source = this.source.filter(x -> {
            int entityId = x.getId();
            for (int id : ids) {
                if (entityId == id) return true;
            }
            return false;
        });

        return (Q) this;
    }

    @Override
    public E first() {
        return source.findFirst().orElse(null);
    }

    @Override
    public E firstReachable() {
        return source.filter(IEntity::isReachable).findFirst().orElse(null);
    }

    @Override
    public E nearest() {
        return nearest(Integer.MAX_VALUE);
    }

    @Override
    public E nearestReachable() {
        return nearestReachable(Integer.MAX_VALUE);
    }

    @Override
    public E nearestReachable(int maxDistance) {
        try {
            var player = new Rs2PlayerModel();
            WorldPoint playerLoc = player.getWorldLocation();
            if (playerLoc == null) {
                return null;
            }
            WorldView worldView = player.getWorldView();

            return pickNearestReachable(byDistance(source, playerLoc, maxDistance), worldView != null);
        } catch (RuntimeException e) {
            return returnNullIfInterrupted(e);
        }
    }

    /**
     * The entities within {@code maxDistance} of {@code from}, nearest first. The sort is stable,
     * so equally near entities keep their stream order (as {@code Stream.min} did).
     */
    static <E extends IEntity> List<EntityDistance<E>> byDistance(Stream<E> entities, WorldPoint from, int maxDistance) {
        List<EntityDistance<E>> result = new ArrayList<>();
        entities.forEach(entity -> {
            WorldPoint loc = entity.getWorldLocation();
            int distance = (loc != null) ? loc.distanceTo(from) : Integer.MAX_VALUE;
            if (distance <= maxDistance) {
                result.add(new EntityDistance<>(entity, distance));
            }
        });
        result.sort(Comparator.comparingInt(pair -> pair.distance));
        return result;
    }

    /**
     * The nearest reachable entity of a nearest-first list. Reachability is only asked until the
     * first "yes", which is the same answer as filtering everything and taking the minimum.
     *
     * <p>When reachability cannot be worked out - {@code canJudge} is false because the player has
     * no world view, or the check throws - this does not silently refuse: it falls back to the
     * nearest entity within {@link #REACHABILITY_FALLBACK_DISTANCE} tiles and logs at debug. An
     * interrupt (shutdown) is rethrown for the caller to turn into null.
     */
    static <E extends IEntity> E pickNearestReachable(List<EntityDistance<E>> nearestFirst, boolean canJudge) {
        if (!canJudge) {
            log.debug("Reachability unknown (no player world view); using the nearest entity within {} tiles",
                    REACHABILITY_FALLBACK_DISTANCE);
            return nearestWithin(nearestFirst, REACHABILITY_FALLBACK_DISTANCE);
        }
        for (EntityDistance<E> candidate : nearestFirst) {
            boolean reachable;
            try {
                reachable = candidate.entity.isReachable();
            } catch (RuntimeException e) {
                if (isInterruption(e)) {
                    throw e;
                }
                log.debug("Reachability of entity {} could not be worked out ({}); using the nearest entity within {} tiles",
                        candidate.entity.getId(), e.toString(), REACHABILITY_FALLBACK_DISTANCE);
                return nearestWithin(nearestFirst, REACHABILITY_FALLBACK_DISTANCE);
            }
            if (reachable) {
                return candidate.entity;
            }
        }
        if (!nearestFirst.isEmpty()) {
            EntityDistance<E> nearest = nearestFirst.get(0);
            log.debug("None of {} candidate(s) is reachable; nearest was entity {} at {} tiles",
                    nearestFirst.size(), nearest.entity.getId(), nearest.distance);
        }
        return null;
    }

    private static <E> E nearestWithin(List<EntityDistance<E>> nearestFirst, int distance) {
        if (nearestFirst.isEmpty() || nearestFirst.get(0).distance > distance) {
            return null;
        }
        return nearestFirst.get(0).entity;
    }

    @Override
    public E nearest(int maxDistance) {
        try {
            var player = new Rs2PlayerModel();
            WorldPoint playerLoc = player.getWorldLocation();
            WorldView worldView = player.getWorldView();
            if (playerLoc == null || worldView == null) {
                return null;
            }

            return nearest(playerLoc, maxDistance);
        } catch (RuntimeException e) {
            return returnNullIfInterrupted(e);
        }
    }

    @Override
    public E nearest(WorldPoint anchor, int maxDistance) {
        if (anchor == null) {
            return null;
        }

        try {
            WorldPoint sceneAnchor = toSceneSpace(anchor);
            return source
                    .map(entity -> {
                        WorldPoint loc = entity.getWorldLocation();
                        int distance = (loc != null) ? loc.distanceTo(sceneAnchor) : Integer.MAX_VALUE;
                        return new EntityDistance<>(entity, distance);
                    })
                    .filter(pair -> pair.distance <= maxDistance)
                    .min(Comparator.comparingInt(pair -> pair.distance))
                    .map(pair -> pair.entity)
                    .orElse(null);
        } catch (RuntimeException e) {
            return returnNullIfInterrupted(e);
        }
    }

    private E returnNullIfInterrupted(RuntimeException e) {
        if (isInterruption(e)) {
            return null;
        }
        throw e;
    }

    private static boolean isInterruption(RuntimeException e) {
        return Thread.currentThread().isInterrupted() || e.getCause() instanceof InterruptedException;
    }

    /**
     * A caller's anchor, in the coordinates the models use. Inside an instance a template point
     * (from {@code Rs2Player.getWorldLocation()} or a hard-coded tile) is moved onto the scene;
     * anything else comes back unchanged. Without a client (unit tests) the anchor is used as is.
     */
    private static WorldPoint toSceneSpace(WorldPoint anchor) {
        Client client = ProjectX.getClient();
        ClientThread clientThread = ProjectX.getClientThread();
        if (client == null || clientThread == null) {
            return anchor;
        }
        try {
            WorldPoint converted = clientThread.invoke(() -> SceneSpace.toSceneSpace(client.getTopLevelWorldView(), anchor));
            return converted != null ? converted : anchor;
        } catch (RuntimeException e) {
            // Interrupted or the client thread is stalled: the anchor as given is still a usable
            // answer outside instances, and the stream's own error handling sees any shutdown.
            log.debug("Could not map anchor {} onto the scene ({}); using it as given", anchor, e.toString());
            return anchor;
        }
    }

    /**
     * Picks the entity for {@code interact(...)}: the nearest reachable one, worked out in one
     * client-thread read (one consistent tick, no hop per entity). The click itself happens on the
     * calling thread, as it may wait.
     */
    private E selectForInteract(int maxDistance) {
        ClientThread clientThread = ProjectX.getClientThread();
        if (clientThread == null) {
            return nearestReachable(maxDistance);
        }
        try {
            return clientThread.invoke(() -> nearestReachable(maxDistance));
        } catch (RuntimeException e) {
            return returnNullIfInterrupted(e);
        }
    }

    @Override
    public List<E> toList() {
        return source.collect(Collectors.toList());
    }

    @Override
    public int count() {
        return (int) source.count();
    }

    public E firstOnClientThread() {
        return ProjectX.getClientThread().invoke(() -> first());
    }

    public E nearestOnClientThread() {
        return ProjectX.getClientThread().invoke(() -> nearest());
    }

    public E nearestOnClientThread(int maxDistance) {
        return ProjectX.getClientThread().invoke(() -> nearest(maxDistance));
    }

    public E nearestOnClientThread(WorldPoint anchor, int maxDistance) {
        return ProjectX.getClientThread().invoke(() -> nearest(anchor, maxDistance));
    }

    public List<E> toListOnClientThread() {
        return ProjectX.getClientThread().invoke(() -> toList());
    }

    public boolean interact() {
        E entity = selectForInteract(Integer.MAX_VALUE);
        if (entity == null) return false;

        return entity.click();
    }

    public boolean interact(String action) {
        E entity = selectForInteract(Integer.MAX_VALUE);
        if (entity == null) return false;
        return entity.click(action);
    }

    public boolean interact(String action, int maxDistance) {
        E entity = selectForInteract(maxDistance);
        if (entity == null) return false;
        return entity.click(action);
    }

    public boolean interact(int id) {
        withId(id);
        E entity = selectForInteract(Integer.MAX_VALUE);
        if (entity == null) return false;
        return entity.click();
    }

    public boolean interact(int id, String action) {
        withId(id);
        E entity = selectForInteract(Integer.MAX_VALUE);
        if (entity == null) return false;
        return entity.click(action);
    }

    public boolean interact(int id, String action, int maxDistance) {
        withId(id);
        E entity = selectForInteract(maxDistance);
        if (entity == null) return false;
        return entity.click(action);
    }
}



class EntityDistance<E> {
    final E entity;
    final int distance;

    EntityDistance(E entity, int distance) {
        this.entity = entity;
        this.distance = distance;
    }
}