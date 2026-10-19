package net.runelite.client.plugins.projectx.scriptqueue;

import net.runelite.api.Skill;
import net.runelite.client.plugins.Plugin;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Stand-ins for the client, for the Script Queue's tests: a clock that moves only when told, a game, and a host. */
final class QueueFakes
{
    private QueueFakes()
    {
    }

    /** A clock that only moves when the test (or a fake sleep) moves it. */
    static final class MutableClock extends Clock
    {
        private Instant now;
        private final ZoneId zone;

        MutableClock(Instant now, ZoneId zone)
        {
            this.now = now;
            this.zone = zone;
        }

        void advance(long millis)
        {
            now = now.plusMillis(millis);
        }

        @Override
        public ZoneId getZone()
        {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone)
        {
            return new MutableClock(now, zone);
        }

        @Override
        public Instant instant()
        {
            return now;
        }
    }

    /** A game whose readings the test sets; anything not set is unknown. */
    static final class FakeGame implements QueueGameState
    {
        final Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        final Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        final Map<String, Integer> inventory = new HashMap<>();
        final Map<String, Integer> bank = new HashMap<>();
        int total = UNKNOWN;
        int combat = UNKNOWN;
        boolean loggedIn = true;
        boolean bankSeen = true;

        @Override
        public int level(Skill skill)
        {
            return loggedIn ? levels.getOrDefault(skill, UNKNOWN) : UNKNOWN;
        }

        @Override
        public int xp(Skill skill)
        {
            return loggedIn ? xp.getOrDefault(skill, UNKNOWN) : UNKNOWN;
        }

        @Override
        public int totalLevel()
        {
            return loggedIn ? total : UNKNOWN;
        }

        @Override
        public int combatLevel()
        {
            return loggedIn ? combat : UNKNOWN;
        }

        @Override
        public int inventoryCount(String item)
        {
            return loggedIn ? inventory.getOrDefault(item.toLowerCase(), 0) : UNKNOWN;
        }

        @Override
        public int bankCount(String item)
        {
            return bankSeen ? bank.getOrDefault(item.toLowerCase(), 0) : UNKNOWN;
        }
    }

    /** A script the fake host can start: it may refuse to start, stop by itself after a while, or log an error. */
    static final class FakeScript extends Plugin
    {
        final String className;
        /** Starts that don't take (the script never comes on), before one that does. */
        int failingStarts;
        /** Stops by itself this long after starting; -1 never. */
        long stopsAfterMs = -1;
        /** The error it logged before stopping by itself, if any. */
        String error;

        boolean active;
        long startedAt;
        int starts;
        final List<Boolean> stops = new ArrayList<>();

        FakeScript(String className)
        {
            this.className = className;
        }
    }

    static final class FakeHost implements QueueHost
    {
        final MutableClock clock;
        final Map<String, FakeScript> scripts = new HashMap<>();
        final List<Object> events = new ArrayList<>();
        /** Run after every fake sleep: the test changes the game as time passes. */
        Runnable onTick = () -> { };
        boolean loggedOut;

        FakeHost(MutableClock clock)
        {
            this.clock = clock;
        }

        FakeScript script(String className)
        {
            return scripts.computeIfAbsent(className, FakeScript::new);
        }

        @Override
        public Plugin find(QueueStep step)
        {
            return scripts.get(step.getPluginClass());
        }

        @Override
        public boolean isActive(Plugin plugin)
        {
            FakeScript s = (FakeScript) plugin;
            if (s.active && s.stopsAfterMs >= 0 && clock.millis() - s.startedAt >= s.stopsAfterMs)
            {
                s.active = false;
            }
            return s.active;
        }

        @Override
        public void start(Plugin plugin)
        {
            FakeScript s = (FakeScript) plugin;
            s.starts++;
            if (s.failingStarts > 0)
            {
                s.failingStarts--;
                return;
            }
            s.active = true;
            s.startedAt = clock.millis();
        }

        @Override
        public void stop(Plugin plugin, boolean manual)
        {
            FakeScript s = (FakeScript) plugin;
            s.active = false;
            s.stops.add(manual);
        }

        @Override
        public boolean onBreak()
        {
            return false;
        }

        @Override
        public String breakStatus()
        {
            return "";
        }

        @Override
        public boolean takeBreak(BooleanSupplier stopRequested, Consumer<String> status)
        {
            return false;
        }

        @Override
        public void logout()
        {
            loggedOut = true;
        }

        @Override
        public void post(Object event)
        {
            events.add(event);
        }

        @Override
        public String errorSince(Plugin plugin, long sinceMillis)
        {
            return ((FakeScript) plugin).error;
        }

        @Override
        public void sleep(long millis)
        {
            clock.advance(millis);
            onTick.run();
        }

        @Override
        public Clock clock()
        {
            return clock;
        }

        <T> List<T> events(Class<T> type)
        {
            return events.stream().filter(type::isInstance).map(type::cast).collect(Collectors.toList());
        }
    }

    /** Collects what the runner says. */
    static final class Listener implements ScriptQueueRunner.Listener
    {
        final List<String> statuses = new ArrayList<>();
        final List<String> steps = new ArrayList<>();

        @Override
        public void status(String text)
        {
            statuses.add(text);
        }

        @Override
        public void step(int index, ScriptQueueRunner.StepState state, String text)
        {
            steps.add(index + " " + state + " " + text);
        }
    }

    static QueueStep step(String className, String name, StopCondition... conditions)
    {
        QueueStep s = new QueueStep();
        s.setPluginClass(className);
        s.setPluginName(name);
        s.setConditions(List.of(conditions));
        return s;
    }
}
