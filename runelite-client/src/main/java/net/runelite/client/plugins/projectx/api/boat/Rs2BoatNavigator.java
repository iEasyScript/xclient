package net.runelite.client.plugins.projectx.api.boat;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.CollisionData;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.api.boat.data.Heading;
import net.runelite.client.plugins.projectx.api.boat.data.PortLocation;
import net.runelite.client.plugins.projectx.api.boat.data.PortPaths;
import net.runelite.client.plugins.projectx.api.boat.models.Rs2BoatModel;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;
import net.runelite.client.plugins.projectx.util.walker.WalkerState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import static net.runelite.client.plugins.projectx.util.Global.sleep;
import static net.runelite.client.plugins.projectx.util.Global.sleepUntil;

/**
 * Sails the player's boat to a place, the way {@code Rs2Walker} walks the player there.
 *
 * <p>Long trips follow the sea lanes: every stored port-to-port route ({@link PortPaths}) is a
 * known-safe line of water, and together they make a network the boat can be routed across with a
 * shortest-path search. The ends of the trip (from the boat to the network, and from the network
 * to the destination), and anything closer than a scene away, are planned on the loaded scene
 * itself: the sea is the tiles a player cannot stand on and nothing is built on, and the boat is
 * kept a tile clear of the shore so it goes round headlands and islands instead of into them.
 *
 * <p>The boat is steered in the game's 16 headings, at a point some way ahead on the planned line,
 * and a boat that stops making progress is turned towards the most open water and tried again.
 *
 * <p>Walking calls end up here on their own: {@code Rs2Walker.walkTo} sails when the player is on a
 * boat and the destination is at sea, and goes ashore first when it is on land. Call
 * {@link #step} once per loop pass from a script that runs its own loop, or {@link #sailTo} to
 * block until the boat gets there.
 *
 * <p>All methods are for script threads, not the client thread.
 *
 * @deprecated Use {@link net.runelite.client.plugins.projectx.bot.BoatNavigator} instead: same methods, shorter name. This name keeps working.
 */
@Slf4j
@Deprecated
public class Rs2BoatNavigator {
    /** Main-world x coordinates stay below this; boats and instances live above it. */
    private static final int MAIN_WORLD_MAX_X = 6400;
    /** A boat turns wide: never call it arrived closer than this. */
    private static final int MIN_ARRIVE_DISTANCE = 4;
    /** A sea-lane waypoint counts as passed within this many tiles. */
    private static final int LANE_REACHED = 8;
    /** Closer than this the trip is planned on the scene alone, without the sea lanes. */
    private static final int DIRECT_RANGE = 48;
    /** The boat is pointed at a planned tile at most this far ahead. */
    private static final int LOOK_AHEAD = 18;
    /** Lanes from different routes closer than this are joined, so routes can be changed between. */
    private static final int LANE_JOIN_DISTANCE = 10;
    /** Further than this from the nearest lane, the lanes are not used. */
    private static final int LANE_MAX_ENTRY = 120;
    private static final long STUCK_AFTER_MS = 6_000;
    private static final long RECOVERY_MS = 3_000;
    private static final int MAX_RECOVERIES = 4;
    private static final long HEADING_RESEND_MS = 2_400;
    private static final long SAIL_TIMEOUT_MS = 20 * 60_000;
    private static final int STEP_SLEEP_MS = 600;
    private static final int GANGPLANK_SEARCH_DISTANCE = 15;

    private static final int SEA_FLAG = CollisionDataFlag.BLOCK_MOVEMENT_FLOOR;
    private static final int OBSTACLE_FLAGS = CollisionDataFlag.BLOCK_MOVEMENT_OBJECT
            | CollisionDataFlag.BLOCK_MOVEMENT_FLOOR_DECORATION;

    private static final Object LOCK = new Object();

    // The trip under way. Guarded by LOCK.
    private static WorldPoint routeTarget;
    private static List<WorldPoint> waypoints = List.of();
    private static int waypointIndex;
    private static WorldPoint progressFrom;
    private static long progressAt;
    private static int recoveries;
    private static long recoveringUntil;
    private static Heading lastHeading;
    private static long lastHeadingAt;

    // The sea-lane network, built for one Sailing level at a time.
    private static LaneGraph lanes;

    protected Rs2BoatNavigator() {
    }

    // ------------------------------------------------------------------ where things are

    /** The boat the player is on, or null when they are not on one. */
    public static Rs2BoatModel getBoat() {
        Rs2BoatCache cache = ProjectX.getRs2BoatCache();
        return cache == null ? null : cache.getLocalBoat();
    }

    /** True while the player is aboard a boat. */
    public static boolean isAboard() {
        return getBoat() != null;
    }

    /**
     * True for a point in the main world. A player on a boat reads coordinates in the boat's own
     * world (tens of thousands of tiles away), which no walk or sail target ever means.
     */
    public static boolean isMainWorld(WorldPoint point) {
        return point != null && point.getX() > 0 && point.getX() < MAIN_WORLD_MAX_X;
    }

    /** Where the boat is in the main world, or null when the player is not on one. */
    public static WorldPoint getBoatLocation() {
        Client client = ProjectX.getClient();
        return ProjectX.getClientThread().runOnClientThreadOptional(() -> {
            Player player = client.getLocalPlayer();
            if (player == null) {
                return null;
            }
            WorldView view = player.getWorldView();
            if (view == null || view.isTopLevel()) {
                return null;
            }
            WorldEntity boat = client.getTopLevelWorldView().worldEntities().byIndex(view.getId());
            if (boat == null || boat.getLocalLocation() == null) {
                return null;
            }
            return WorldPoint.fromLocalInstance(client, boat.getLocalLocation());
        }).orElse(null);
    }

    // ------------------------------------------------------------------ sailing

    /**
     * Sails to {@code target} and stops there, blocking until the boat arrives, gets stuck for good
     * or the thread is interrupted.
     *
     * @param distance how close counts as there (at least {@value #MIN_ARRIVE_DISTANCE})
     * @return {@link WalkerState#ARRIVED}, {@link WalkerState#UNREACHABLE} when the boat could not
     * get there, or {@link WalkerState#EXIT} when not on a boat, logged out or interrupted
     */
    public static WalkerState sailTo(WorldPoint target, int distance) {
        if (!isMainWorld(target)) {
            log.warn("[Boat] sail rejected: {} is not in the main world", target);
            return WalkerState.EXIT;
        }
        long started = System.currentTimeMillis();
        while (true) {
            if (Thread.currentThread().isInterrupted() || !ProjectX.isLoggedIn()) {
                return WalkerState.EXIT;
            }
            WalkerState state = step(target, distance);
            if (state == WalkerState.ARRIVED) {
                stopSailing();
                return state;
            }
            if (state != WalkerState.MOVING) {
                stopSailing();
                return state;
            }
            if (System.currentTimeMillis() - started > SAIL_TIMEOUT_MS) {
                log.warn("[Boat] gave up sailing to {} after {} minutes", target, SAIL_TIMEOUT_MS / 60_000);
                reset();
                stopSailing();
                return WalkerState.UNREACHABLE;
            }
            sleep(STEP_SLEEP_MS);
        }
    }

    /** {@link #sailTo(WorldPoint, int)} with the shortest arrival distance. */
    public static WalkerState sailTo(WorldPoint target) {
        return sailTo(target, MIN_ARRIVE_DISTANCE);
    }

    /**
     * One pass of sailing to {@code target}: takes the helm if needed, steers, and checks for being
     * stuck. Returns at once; call it again on the next loop pass. The sails are left up on
     * arrival, so the caller decides whether to stop ({@link #stopSailing}) or carry on.
     *
     * @return {@link WalkerState#MOVING} while under way, {@link WalkerState#ARRIVED} within
     * {@code distance}, {@link WalkerState#UNREACHABLE} when stuck for good, {@link WalkerState#EXIT}
     * when the player is not on a boat
     */
    public static WalkerState step(WorldPoint target, int distance) {
        Rs2BoatModel boat = getBoat();
        if (boat == null || !isMainWorld(target)) {
            return WalkerState.EXIT;
        }
        WorldPoint here = getBoatLocation();
        if (here == null) {
            // Boarding or the scene reloading: try again next pass.
            return WalkerState.MOVING;
        }
        int arrive = Math.max(distance, MIN_ARRIVE_DISTANCE);
        if (here.distanceTo2D(target) <= arrive) {
            reset();
            return WalkerState.ARRIVED;
        }
        if (!boat.isNavigating()) {
            if (!takeHelm(boat)) {
                log.warn("[Boat] could not take the helm");
            }
            return WalkerState.MOVING;
        }

        WorldPoint goal;
        boolean finalLeg;
        long now = System.currentTimeMillis();
        synchronized (LOCK) {
            if (routeTarget == null || !routeTarget.equals(target)) {
                plan(here, target);
            }
            while (waypointIndex < waypoints.size() - 1 && here.distanceTo2D(waypoints.get(waypointIndex)) <= LANE_REACHED) {
                waypointIndex++;
            }
            goal = waypoints.get(waypointIndex);
            finalLeg = waypointIndex == waypoints.size() - 1;

            if (progressFrom == null || here.distanceTo2D(progressFrom) >= 3) {
                progressFrom = here;
                progressAt = now;
            } else if (now - progressAt > STUCK_AFTER_MS && now >= recoveringUntil) {
                recoveries++;
                progressAt = now;
                if (recoveries > MAX_RECOVERIES) {
                    log.warn("[Boat] stuck at {} on the way to {}; giving up", here, target);
                    reset();
                    return WalkerState.UNREACHABLE;
                }
                Heading away = openWaterHeading(here, boat.getCurrentHeading(), recoveries);
                log.info("[Boat] stuck at {} (try {}): turning {}", here, recoveries, away);
                recoveringUntil = now + RECOVERY_MS;
                steer(boat, away, true);
                return WalkerState.MOVING;
            }
            if (now < recoveringUntil) {
                return WalkerState.MOVING;
            }
        }

        WorldPoint aim = planOnScene(here, goal, finalLeg ? arrive : LANE_REACHED);
        steer(boat, headingTowards(here, aim != null ? aim : goal), false);
        return WalkerState.MOVING;
    }

    /** Lowers the sails, if the player is at the helm. */
    public static void stopSailing() {
        Rs2BoatModel boat = getBoat();
        if (boat != null) {
            boat.unsetSails();
        }
    }

    /** Forgets the trip under way, so the next call plans afresh. */
    public static void reset() {
        synchronized (LOCK) {
            routeTarget = null;
            waypoints = List.of();
            waypointIndex = 0;
            progressFrom = null;
            recoveries = 0;
            recoveringUntil = 0;
            lastHeading = null;
        }
    }

    // ------------------------------------------------------------------ the helm and the gangplank

    /** Takes the helm. True once the player is navigating. */
    public static boolean takeHelm(Rs2BoatModel boat) {
        if (boat.isNavigating()) {
            return true;
        }
        if (boat.navigate()) {
            return true;
        }
        // Not the wooden helm navigate() knows: any helm on this boat offering the option.
        ProjectX.getRs2TileObjectCache().query().fromWorldView()
                .where(o -> hasAction(o.getObjectComposition(), "Navigate"))
                .interact("Navigate");
        return sleepUntil(boat::isNavigating, 5000);
    }

    /** Lowers the sails and lets go of the helm. True once the player is no longer steering. */
    public static boolean leaveHelm(Rs2BoatModel boat) {
        boat.unsetSails();
        if (!boat.isNavigating()) {
            return true;
        }
        ProjectX.getRs2TileObjectCache().query().fromWorldView()
                .where(o -> hasAction(o.getObjectComposition(), "Let go"))
                .interact("Let go");
        return sleepUntil(() -> !boat.isNavigating(), 3000);
    }

    /**
     * Gets the player off the boat for a walk to {@code destination} on land: straight down the
     * gangplank when moored, otherwise sails to the port nearest the destination first.
     *
     * @return {@link WalkerState#ARRIVED} once ashore
     */
    public static WalkerState goAshore(WorldPoint destination) {
        Rs2BoatModel boat = getBoat();
        if (boat == null) {
            return WalkerState.ARRIVED;
        }
        if (disembark(boat)) {
            return WalkerState.ARRIVED;
        }
        PortLocation port = nearestPort(destination);
        if (port == null) {
            log.warn("[Boat] no port near {} to go ashore at", destination);
            return WalkerState.UNREACHABLE;
        }
        log.info("[Boat] sailing to {} to go ashore for {}", port.getName(), destination);
        WalkerState sailed = sailTo(port.getNavigationLocation(), 5);
        if (sailed != WalkerState.ARRIVED) {
            return sailed;
        }
        Rs2BoatModel moored = getBoat();
        return moored == null || disembark(moored) ? WalkerState.ARRIVED : WalkerState.UNREACHABLE;
    }

    /** Down the gangplank, if there is one in reach. True once off the boat. */
    private static boolean disembark(Rs2BoatModel boat) {
        boolean gangplank = ProjectX.getRs2TileObjectCache().query().fromWorldView()
                .withId(ObjectID.SAILING_GANGPLANK_DISEMBARK)
                .nearest(GANGPLANK_SEARCH_DISTANCE) != null;
        if (!gangplank) {
            return false;
        }
        leaveHelm(boat);
        ProjectX.getRs2TileObjectCache().query().fromWorldView()
                .withId(ObjectID.SAILING_GANGPLANK_DISEMBARK)
                .interact("Disembark");
        return sleepUntil(() -> !isAboard(), 6000);
    }

    private static PortLocation nearestPort(WorldPoint destination) {
        int level = Rs2Player.getRealSkillLevel(Skill.SAILING);
        PortLocation best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (PortLocation port : PortLocation.values()) {
            if (port == PortLocation.EMPTY || port.getSailingLevelRequired() == null
                    || port.getSailingLevelRequired() > level) {
                continue;
            }
            int d = port.getNavigationLocation().distanceTo2D(destination);
            if (d < bestDistance) {
                best = port;
                bestDistance = d;
            }
        }
        return best;
    }

    private static boolean hasAction(net.runelite.api.ObjectComposition composition, String action) {
        if (composition == null || composition.getActions() == null) {
            return false;
        }
        for (String a : composition.getActions()) {
            if (a != null && a.equalsIgnoreCase(action)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ steering

    private static void steer(Rs2BoatModel boat, Heading heading, boolean force) {
        if (heading == null) {
            return;
        }
        long now = System.currentTimeMillis();
        synchronized (LOCK) {
            if (force || heading != lastHeading || now - lastHeadingAt > HEADING_RESEND_MS) {
                if (boat.getCurrentHeading() != heading) {
                    boat.setHeading(heading);
                }
                lastHeading = heading;
                lastHeadingAt = now;
            }
        }
        if (!boat.isMovingForward()) {
            boat.setSails();
        }
    }

    /** The nearest of the 16 headings from one point towards another. */
    static Heading headingTowards(WorldPoint from, WorldPoint to) {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        if (dx == 0 && dy == 0) {
            return null;
        }
        double angle = Math.toDegrees(Math.atan2(dy, dx));
        double heading = (270.0 - angle + 360.0) % 360.0;
        return Heading.getHeading((int) ((heading + 11.25) / 22.5) & 0xF);
    }

    private static int[] headingVector(Heading heading, int length) {
        double angle = Math.toRadians(270.0 - heading.getValue() * 22.5);
        return new int[]{(int) Math.round(Math.cos(angle) * length), (int) Math.round(Math.sin(angle) * length)};
    }

    /**
     * The heading with the most open water ahead, for getting unstuck. Without a usable scene,
     * alternate quarter turns.
     */
    private static Heading openWaterHeading(WorldPoint here, Heading current, int attempt) {
        SeaGrid grid = SeaGrid.capture();
        int currentValue = current == null ? 0 : current.getValue();
        if (grid != null && grid.contains(here)) {
            Heading best = null;
            int bestRun = -1;
            for (Heading h : Heading.values()) {
                int diff = Math.abs(h.getValue() - currentValue);
                if (Math.min(diff, 16 - diff) <= 1) {
                    continue; // the way it is already failing to go
                }
                int run = 0;
                for (int i = 1; i <= 12; i++) {
                    int[] v = headingVector(h, i);
                    if (!grid.isSea(here.getX() + v[0], here.getY() + v[1])) {
                        break;
                    }
                    run++;
                }
                if (run > bestRun) {
                    best = h;
                    bestRun = run;
                }
            }
            if (best != null && bestRun > 2) {
                return best;
            }
        }
        int turn = attempt % 2 == 1 ? 4 : 12;
        return Heading.getHeading((currentValue + turn) & 0xF);
    }

    // ------------------------------------------------------------------ planning the trip

    /** Waypoints for the whole trip: the sea lanes for long ones, the target alone otherwise. */
    private static void plan(WorldPoint here, WorldPoint target) {
        routeTarget = target;
        waypointIndex = 0;
        progressFrom = null;
        recoveries = 0;
        recoveringUntil = 0;
        lastHeading = null;
        List<WorldPoint> route = here.distanceTo2D(target) <= DIRECT_RANGE ? null : laneRoute(here, target);
        if (route == null || route.isEmpty()) {
            waypoints = List.of(target);
            log.info("[Boat] sailing to {} directly from {}", target, here);
        } else {
            waypoints = route;
            log.info("[Boat] sailing to {} from {} along the sea lanes ({} waypoints)", target, here, route.size());
        }
    }

    private static List<WorldPoint> laneRoute(WorldPoint here, WorldPoint target) {
        int level = Rs2Player.getRealSkillLevel(Skill.SAILING);
        LaneGraph graph = lanes;
        if (graph == null || graph.level != level) {
            graph = LaneGraph.build(level);
            lanes = graph;
        }
        int entry = graph.nearest(here);
        int exit = graph.nearest(target);
        if (entry < 0 || exit < 0
                || graph.nodes.get(entry).distanceTo2D(here) > LANE_MAX_ENTRY
                || graph.nodes.get(exit).distanceTo2D(target) > LANE_MAX_ENTRY) {
            return null;
        }
        List<WorldPoint> path = graph.shortestPath(entry, exit);
        if (path == null) {
            return null;
        }
        // Cut the lane where it passes closest to the target, then finish off the lane.
        int closest = 0;
        for (int i = 1; i < path.size(); i++) {
            if (path.get(i).distanceTo2D(target) < path.get(closest).distanceTo2D(target)) {
                closest = i;
            }
        }
        List<WorldPoint> route = new ArrayList<>(path.subList(0, closest + 1));
        route.add(target);
        return route;
    }

    /**
     * Plans from the boat towards {@code goal} on the loaded scene and returns the tile to steer at:
     * the furthest planned tile within {@value #LOOK_AHEAD} that the boat can sail to in a straight
     * line. Null when the scene cannot be read as sea and land (then the boat steers straight).
     */
    private static WorldPoint planOnScene(WorldPoint here, WorldPoint goal, int reach) {
        SeaGrid grid = SeaGrid.capture();
        if (grid == null || !grid.contains(here) || !grid.looksLikeSea(here)) {
            return null;
        }
        List<WorldPoint> path = grid.findPath(here, goal, reach);
        if (path == null || path.size() < 2) {
            return null;
        }
        WorldPoint aim = path.get(1);
        for (int i = path.size() - 1; i >= 1; i--) {
            WorldPoint p = path.get(i);
            if (here.distanceTo2D(p) <= LOOK_AHEAD && grid.clearLine(here, p)) {
                aim = p;
                break;
            }
        }
        return aim;
    }

    /** The loaded scene as sea and not-sea, for planning round the shore. */
    private static final class SeaGrid {
        private static final int[][] DIRECTIONS = {
                {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
        };
        private static final int SEARCH_LIMIT = 15_000;

        final int baseX;
        final int baseY;
        final int width;
        final int height;
        final boolean[][] sea;

        private SeaGrid(int baseX, int baseY, boolean[][] sea) {
            this.baseX = baseX;
            this.baseY = baseY;
            this.sea = sea;
            this.width = sea.length;
            this.height = width == 0 ? 0 : sea[0].length;
        }

        static SeaGrid capture() {
            Client client = ProjectX.getClient();
            return ProjectX.getClientThread().runOnClientThreadOptional(() -> {
                WorldView top = client.getTopLevelWorldView();
                if (top == null || top.isInstance()) {
                    return null;
                }
                CollisionData[] maps = top.getCollisionMaps();
                if (maps == null || maps.length == 0 || maps[0] == null) {
                    return null;
                }
                int[][] flags = maps[0].getFlags();
                boolean[][] sea = new boolean[flags.length][];
                for (int x = 0; x < flags.length; x++) {
                    sea[x] = new boolean[flags[x].length];
                    for (int y = 0; y < flags[x].length; y++) {
                        int f = flags[x][y];
                        sea[x][y] = (f & SEA_FLAG) != 0 && (f & OBSTACLE_FLAGS) == 0;
                    }
                }
                return new SeaGrid(top.getBaseX(), top.getBaseY(), sea);
            }).orElse(null);
        }

        boolean contains(WorldPoint p) {
            return inGrid(p.getX() - baseX, p.getY() - baseY);
        }

        private boolean inGrid(int sx, int sy) {
            return sx >= 0 && sy >= 0 && sx < width && sy < height;
        }

        boolean isSea(int worldX, int worldY) {
            int sx = worldX - baseX;
            int sy = worldY - baseY;
            return inGrid(sx, sy) && sea[sx][sy];
        }

        /**
         * A check that this scene marks its sea the way the planner expects: most of the water
         * round the boat reads as sea. If not, planning on it would only steer the boat wrong.
         */
        boolean looksLikeSea(WorldPoint here) {
            int seaTiles = 0;
            int tiles = 0;
            for (int dx = -3; dx <= 3; dx++) {
                for (int dy = -3; dy <= 3; dy++) {
                    tiles++;
                    if (isSea(here.getX() + dx, here.getY() + dy)) {
                        seaTiles++;
                    }
                }
            }
            return seaTiles * 2 >= tiles;
        }

        /** Sea with sea all round it: room for the hull. */
        private boolean roomy(int sx, int sy) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    int x = sx + dx;
                    int y = sy + dy;
                    if (!inGrid(x, y) || !sea[x][y]) {
                        return false;
                    }
                }
            }
            return true;
        }

        boolean clearLine(WorldPoint from, WorldPoint to) {
            int x0 = from.getX() - baseX;
            int y0 = from.getY() - baseY;
            int x1 = to.getX() - baseX;
            int y1 = to.getY() - baseY;
            int dx = Math.abs(x1 - x0);
            int dy = -Math.abs(y1 - y0);
            int sx = x0 < x1 ? 1 : -1;
            int sy = y0 < y1 ? 1 : -1;
            int err = dx + dy;
            int steps = 0;
            while (true) {
                // The first tiles are where the boat already is, roomy or not.
                if (steps > 2 && (!inGrid(x0, y0) || !roomy(x0, y0))) {
                    return false;
                }
                if (x0 == x1 && y0 == y1) {
                    return true;
                }
                int e2 = 2 * err;
                if (e2 >= dy) {
                    err += dy;
                    x0 += sx;
                }
                if (e2 <= dx) {
                    err += dx;
                    y0 += sy;
                }
                steps++;
            }
        }

        /**
         * A* from the boat towards {@code goal}, over roomy sea. The goal may be off the scene or on
         * land: the search then ends at the reachable tile closest to it. Near the start and the
         * goal, plain sea is enough (a moored boat sits against the dock).
         */
        List<WorldPoint> findPath(WorldPoint start, WorldPoint goal, int reach) {
            int startX = start.getX() - baseX;
            int startY = start.getY() - baseY;
            int goalX = goal.getX() - baseX;
            int goalY = goal.getY() - baseY;
            int size = width * height;
            double[] cost = new double[size];
            Arrays.fill(cost, Double.MAX_VALUE);
            int[] parent = new int[size];
            Arrays.fill(parent, -1);
            boolean[] closed = new boolean[size];

            int startIndex = startX * height + startY;
            cost[startIndex] = 0;
            PriorityQueue<double[]> open = new PriorityQueue<>((a, b) -> Double.compare(a[0], b[0]));
            open.add(new double[]{heuristic(startX, startY, goalX, goalY), startIndex});
            int best = startIndex;
            double bestDistance = heuristic(startX, startY, goalX, goalY);
            int expanded = 0;

            while (!open.isEmpty() && expanded < SEARCH_LIMIT) {
                int index = (int) open.poll()[1];
                if (closed[index]) {
                    continue;
                }
                closed[index] = true;
                expanded++;
                int x = index / height;
                int y = index % height;
                double toGoal = heuristic(x, y, goalX, goalY);
                if (toGoal < bestDistance) {
                    best = index;
                    bestDistance = toGoal;
                }
                if (Math.max(Math.abs(x - goalX), Math.abs(y - goalY)) <= reach) {
                    best = index;
                    break;
                }
                for (int[] d : DIRECTIONS) {
                    int nx = x + d[0];
                    int ny = y + d[1];
                    if (!inGrid(nx, ny) || !sea[nx][ny]) {
                        continue;
                    }
                    boolean nearEnds = Math.max(Math.abs(nx - startX), Math.abs(ny - startY)) <= 3
                            || Math.max(Math.abs(nx - goalX), Math.abs(ny - goalY)) <= reach + 2;
                    if (!nearEnds && !roomy(nx, ny)) {
                        continue;
                    }
                    if (d[0] != 0 && d[1] != 0 && (!sea[x + d[0]][y] || !sea[x][y + d[1]])) {
                        continue; // no cutting a corner of land
                    }
                    int next = nx * height + ny;
                    double step = d[0] != 0 && d[1] != 0 ? Math.sqrt(2) : 1;
                    double nextCost = cost[index] + step;
                    if (nextCost < cost[next]) {
                        cost[next] = nextCost;
                        parent[next] = index;
                        open.add(new double[]{nextCost + heuristic(nx, ny, goalX, goalY), next});
                    }
                }
            }
            if (best == startIndex) {
                return null;
            }
            ArrayDeque<WorldPoint> path = new ArrayDeque<>();
            for (int i = best; i != -1; i = parent[i]) {
                path.addFirst(new WorldPoint(baseX + i / height, baseY + i % height, 0));
            }
            return new ArrayList<>(path);
        }

        private static double heuristic(int x, int y, int goalX, int goalY) {
            int dx = Math.abs(x - goalX);
            int dy = Math.abs(y - goalY);
            return Math.max(dx, dy) + (Math.sqrt(2) - 1) * Math.min(dx, dy);
        }
    }

    /**
     * The stored port routes joined into one network. Nodes are route points; edges join the
     * points of each route in order, and points of different routes that come close together.
     * Routes to ports the player lacks the level for are left out.
     */
    private static final class LaneGraph {
        final int level;
        final List<WorldPoint> nodes = new ArrayList<>();
        final List<List<int[]>> edges = new ArrayList<>();

        private LaneGraph(int level) {
            this.level = level;
        }

        static LaneGraph build(int level) {
            LaneGraph graph = new LaneGraph(level);
            Map<WorldPoint, Integer> index = new HashMap<>();
            for (PortPaths route : PortPaths.values()) {
                Integer from = route.getStart().getSailingLevelRequired();
                Integer to = route.getEnd().getSailingLevelRequired();
                if (route.getStart() == PortLocation.EMPTY || route.getEnd() == PortLocation.EMPTY
                        || from == null || to == null || from > level || to > level) {
                    continue;
                }
                int previous = -1;
                for (WorldPoint p : route.getFullPath(false)) {
                    int node = index.computeIfAbsent(p, k -> graph.add(k));
                    if (previous >= 0 && previous != node) {
                        graph.join(previous, node);
                    }
                    previous = node;
                }
            }
            for (int a = 0; a < graph.nodes.size(); a++) {
                for (int b = a + 1; b < graph.nodes.size(); b++) {
                    if (graph.nodes.get(a).distanceTo2D(graph.nodes.get(b)) <= LANE_JOIN_DISTANCE) {
                        graph.join(a, b);
                    }
                }
            }
            log.debug("[Boat] sea lanes for Sailing {}: {} points", level, graph.nodes.size());
            return graph;
        }

        private int add(WorldPoint p) {
            nodes.add(p);
            edges.add(new ArrayList<>());
            return nodes.size() - 1;
        }

        private void join(int a, int b) {
            int weight = (int) Math.round(Math.hypot(
                    nodes.get(a).getX() - nodes.get(b).getX(), nodes.get(a).getY() - nodes.get(b).getY()));
            edges.get(a).add(new int[]{b, weight});
            edges.get(b).add(new int[]{a, weight});
        }

        int nearest(WorldPoint p) {
            int best = -1;
            int bestDistance = Integer.MAX_VALUE;
            for (int i = 0; i < nodes.size(); i++) {
                int d = nodes.get(i).distanceTo2D(p);
                if (d < bestDistance) {
                    best = i;
                    bestDistance = d;
                }
            }
            return best;
        }

        List<WorldPoint> shortestPath(int from, int to) {
            int[] distance = new int[nodes.size()];
            int[] parent = new int[nodes.size()];
            Arrays.fill(distance, Integer.MAX_VALUE);
            Arrays.fill(parent, -1);
            distance[from] = 0;
            PriorityQueue<int[]> queue = new PriorityQueue<>((a, b) -> Integer.compare(a[0], b[0]));
            queue.add(new int[]{0, from});
            while (!queue.isEmpty()) {
                int[] head = queue.poll();
                int node = head[1];
                if (head[0] > distance[node]) {
                    continue;
                }
                if (node == to) {
                    break;
                }
                for (int[] edge : edges.get(node)) {
                    int next = distance[node] + edge[1];
                    if (next < distance[edge[0]]) {
                        distance[edge[0]] = next;
                        parent[edge[0]] = node;
                        queue.add(new int[]{next, edge[0]});
                    }
                }
            }
            if (distance[to] == Integer.MAX_VALUE) {
                return null;
            }
            ArrayDeque<WorldPoint> path = new ArrayDeque<>();
            for (int i = to; i != -1; i = parent[i]) {
                path.addFirst(nodes.get(i));
            }
            return new ArrayList<>(path);
        }
    }
}
