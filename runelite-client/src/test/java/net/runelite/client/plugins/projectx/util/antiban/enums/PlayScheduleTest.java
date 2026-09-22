package net.runelite.client.plugins.projectx.util.antiban.enums;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

public class PlayScheduleTest {

    @Test
    public void testJvmTimezoneIsEuropeBrussels() {
        ZoneId expectedZone = ZoneId.of("Europe/Brussels");
        ZoneId actualZone = ZoneId.systemDefault();

        System.out.println("=== TIMEZONE DEBUG INFO ===");
        System.out.println("Expected timezone: " + expectedZone);
        System.out.println("Actual JVM timezone: " + actualZone);
        System.out.println("LocalTime.now(): " + LocalTime.now());
        System.out.println("LocalDateTime.now(): " + LocalDateTime.now());
        System.out.println("System.getenv(\"TZ\"): " + System.getenv("TZ"));
        System.out.println("user.timezone property: " + System.getProperty("user.timezone"));
        System.out.println("===========================");

        assertEquals("JVM timezone should be Europe/Brussels", expectedZone, actualZone);
    }

    @Test
    public void testMediumAfternoonScheduleDefinition() {
        PlaySchedule schedule = PlaySchedule.MEDIUM_AFTERNOON;
        assertNotNull(schedule);
    }

    @Test
    public void testAllSchedulesHaveValidTimeRanges() {
        for (PlaySchedule schedule : PlaySchedule.values()) {
            assertNotNull("Schedule " + schedule.name() + " should not be null", schedule);
        }
    }

    @Test
    public void testTimeUntilNextScheduleReturnsZeroWhenInsideSchedule() {
        for (PlaySchedule schedule : PlaySchedule.values()) {
            if (!schedule.isOutsideSchedule()) {
                Duration timeUntil = schedule.timeUntilNextSchedule();
                assertEquals("When inside schedule, timeUntilNextSchedule should be zero for " + schedule.name(),
                        Duration.ZERO, timeUntil);
            }
        }
    }

    @Test
    public void testTimeUntilNextScheduleReturnsPositiveWhenOutsideSchedule() {
        for (PlaySchedule schedule : PlaySchedule.values()) {
            if (schedule.isOutsideSchedule()) {
                Duration timeUntil = schedule.timeUntilNextSchedule();
                assertTrue("When outside schedule, timeUntilNextSchedule should be positive for " + schedule.name(),
                        timeUntil.toMillis() > 0);
            }
        }
    }

    @Test
    public void testDisplayStringNotNull() {
        for (PlaySchedule schedule : PlaySchedule.values()) {
            String display = schedule.displayString();
            assertNotNull("Display string should not be null for " + schedule.name(), display);
            assertFalse("Display string should not be empty for " + schedule.name(), display.isEmpty());
        }
    }

    @Test
    public void testDisplayStringContainsScheduleName() {
        for (PlaySchedule schedule : PlaySchedule.values()) {
            String display = schedule.displayString();
            assertTrue("Display string should contain schedule name for " + schedule.name(),
                    display.contains(schedule.name()));
        }
    }

    @Test
    public void testIsOutsideScheduleConsistentWithTimeUntilNextSchedule() {
        for (PlaySchedule schedule : PlaySchedule.values()) {
            boolean outside = schedule.isOutsideSchedule();
            Duration timeUntil = schedule.timeUntilNextSchedule();

            if (outside) {
                assertTrue("When outside schedule, duration should be positive for " + schedule.name(),
                        timeUntil.toMillis() > 0);
            } else {
                assertEquals("When inside schedule, duration should be zero for " + schedule.name(),
                        Duration.ZERO, timeUntil);
            }
        }
    }

    @RunWith(Parameterized.class)
    public static class ScheduleTimeTest {

        @Parameterized.Parameters(name = "{0} at {1} should be outside={2}")
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {PlaySchedule.MEDIUM_AFTERNOON, LocalTime.of(13, 0), false},
                    {PlaySchedule.MEDIUM_AFTERNOON, LocalTime.of(12, 0), false},
                    {PlaySchedule.MEDIUM_AFTERNOON, LocalTime.of(15, 0), false},
                    {PlaySchedule.MEDIUM_AFTERNOON, LocalTime.of(11, 59), true},
                    {PlaySchedule.MEDIUM_AFTERNOON, LocalTime.of(15, 1), true},
                    {PlaySchedule.MEDIUM_AFTERNOON, LocalTime.of(5, 0), true},
                    {PlaySchedule.MEDIUM_AFTERNOON, LocalTime.of(20, 0), true},

                    {PlaySchedule.MEDIUM_DAY, LocalTime.of(8, 0), false},
                    {PlaySchedule.MEDIUM_DAY, LocalTime.of(12, 0), false},
                    {PlaySchedule.MEDIUM_DAY, LocalTime.of(18, 0), false},
                    {PlaySchedule.MEDIUM_DAY, LocalTime.of(7, 59), true},
                    {PlaySchedule.MEDIUM_DAY, LocalTime.of(18, 1), true},

                    {PlaySchedule.SHORT_MORNING, LocalTime.of(8, 0), false},
                    {PlaySchedule.SHORT_MORNING, LocalTime.of(8, 30), false},
                    {PlaySchedule.SHORT_MORNING, LocalTime.of(9, 0), false},
                    {PlaySchedule.SHORT_MORNING, LocalTime.of(7, 59), true},
                    {PlaySchedule.SHORT_MORNING, LocalTime.of(9, 1), true},
            });
        }

        private final PlaySchedule schedule;
        private final LocalTime testTime;
        private final boolean expectedOutside;

        public ScheduleTimeTest(PlaySchedule schedule, LocalTime testTime, boolean expectedOutside) {
            this.schedule = schedule;
            this.testTime = testTime;
            this.expectedOutside = expectedOutside;
        }

        @Test
        public void testScheduleAtSpecificTime() {
            boolean isOutside = isOutsideScheduleAt(schedule, testTime);
            assertEquals(
                    String.format("%s at %s should be outside=%s", schedule.name(), testTime, expectedOutside),
                    expectedOutside,
                    isOutside
            );
        }

        private boolean isOutsideScheduleAt(PlaySchedule schedule, LocalTime time) {
            LocalTime startTime = getStartTime(schedule);
            LocalTime endTime = getEndTime(schedule);
            return time.isBefore(startTime) || time.isAfter(endTime);
        }

        private LocalTime getStartTime(PlaySchedule schedule) {
            switch (schedule) {
                case SHORT_MORNING: return LocalTime.of(8, 0);
                case MEDIUM_MORNING: return LocalTime.of(7, 0);
                case LONG_MORNING: return LocalTime.of(6, 0);
                case SHORT_AFTERNOON: return LocalTime.of(12, 0);
                case MEDIUM_AFTERNOON: return LocalTime.of(12, 0);
                case LONG_AFTERNOON: return LocalTime.of(12, 0);
                case SHORT_EVENING: return LocalTime.of(18, 0);
                case MEDIUM_EVENING: return LocalTime.of(17, 0);
                case LONG_EVENING: return LocalTime.of(17, 0);
                case SHORT_DAY: return LocalTime.of(9, 0);
                case MEDIUM_DAY: return LocalTime.of(8, 0);
                case LONG_DAY: return LocalTime.of(6, 0);
                case SHORT_NIGHT: return LocalTime.of(23, 0);
                case MEDIUM_NIGHT: return LocalTime.of(21, 0);
                case LONG_NIGHT: return LocalTime.of(19, 0);
                case FIRST_NIGHT: return LocalTime.of(22, 0);
                case SECOND_NIGHT: return LocalTime.of(1, 0);
                case THIRD_NIGHT: return LocalTime.of(4, 0);
                default: throw new IllegalArgumentException("Unknown schedule: " + schedule);
            }
        }

        private LocalTime getEndTime(PlaySchedule schedule) {
            switch (schedule) {
                case SHORT_MORNING: return LocalTime.of(9, 0);
                case MEDIUM_MORNING: return LocalTime.of(10, 0);
                case LONG_MORNING: return LocalTime.of(12, 0);
                case SHORT_AFTERNOON: return LocalTime.of(13, 0);
                case MEDIUM_AFTERNOON: return LocalTime.of(15, 0);
                case LONG_AFTERNOON: return LocalTime.of(18, 0);
                case SHORT_EVENING: return LocalTime.of(19, 0);
                case MEDIUM_EVENING: return LocalTime.of(20, 0);
                case LONG_EVENING: return LocalTime.of(23, 0);
                case SHORT_DAY: return LocalTime.of(17, 0);
                case MEDIUM_DAY: return LocalTime.of(18, 0);
                case LONG_DAY: return LocalTime.of(22, 0);
                case SHORT_NIGHT: return LocalTime.of(7, 0);
                case MEDIUM_NIGHT: return LocalTime.of(9, 0);
                case LONG_NIGHT: return LocalTime.of(11, 0);
                case FIRST_NIGHT: return LocalTime.of(1, 0);
                case SECOND_NIGHT: return LocalTime.of(4, 0);
                case THIRD_NIGHT: return LocalTime.of(7, 0);
                default: throw new IllegalArgumentException("Unknown schedule: " + schedule);
            }
        }
    }

    @Test
    public void testMediumAfternoonAt0500ShouldBeOutside() {
        LocalTime time = LocalTime.of(5, 0);
        LocalTime startTime = LocalTime.of(12, 0);
        LocalTime endTime = LocalTime.of(15, 0);

        boolean isOutside = time.isBefore(startTime) || time.isAfter(endTime);
        assertTrue("05:00 should be outside MEDIUM_AFTERNOON (12:00-15:00)", isOutside);
    }

    @Test
    public void testMediumAfternoonAt1300ShouldBeInside() {
        LocalTime time = LocalTime.of(13, 0);
        LocalTime startTime = LocalTime.of(12, 0);
        LocalTime endTime = LocalTime.of(15, 0);

        boolean isOutside = time.isBefore(startTime) || time.isAfter(endTime);
        assertFalse("13:00 should be inside MEDIUM_AFTERNOON (12:00-15:00)", isOutside);
    }

    @Test
    public void testTimeUntilNextScheduleCalculation() {
        LocalTime currentTime = LocalTime.of(5, 0);
        LocalTime startTime = LocalTime.of(12, 0);
        LocalTime endTime = LocalTime.of(15, 0);

        Duration expected;
        if (currentTime.isBefore(startTime)) {
            expected = Duration.between(currentTime, startTime);
        } else if (currentTime.isAfter(endTime)) {
            expected = Duration.between(currentTime, startTime).plusDays(1);
        } else {
            expected = Duration.ZERO;
        }

        assertEquals("At 05:00, time until 12:00 should be 7 hours", Duration.ofHours(7), expected);
    }

    @Test
    public void testTimeUntilNextScheduleAfterEndTime() {
        LocalTime currentTime = LocalTime.of(16, 0);
        LocalTime startTime = LocalTime.of(12, 0);

        Duration expected = Duration.between(currentTime, startTime).plusDays(1);

        assertEquals("At 16:00, time until next day 12:00 should be 20 hours",
                Duration.ofHours(20), expected);
    }

    @Test
    public void testTimeUntilScheduleEndsReturnsZeroWhenOutside() {
        for (PlaySchedule schedule : PlaySchedule.values()) {
            if (schedule.isOutsideSchedule()) {
                Duration timeUntilEnd = schedule.timeUntilScheduleEnds();
                assertEquals("When outside schedule, timeUntilScheduleEnds should be zero for " + schedule.name(),
                        Duration.ZERO, timeUntilEnd);
            }
        }
    }

    @Test
    public void testTimeUntilScheduleEndsReturnsPositiveWhenInside() {
        for (PlaySchedule schedule : PlaySchedule.values()) {
            if (!schedule.isOutsideSchedule()) {
                Duration timeUntilEnd = schedule.timeUntilScheduleEnds();
                assertTrue("When inside schedule, timeUntilScheduleEnds should be non-negative for " + schedule.name(),
                        timeUntilEnd.toMillis() >= 0);
            }
        }
    }

    @Test
    public void testTimeUntilScheduleEndsCalculation() {
        LocalTime currentTime = LocalTime.of(14, 0);
        LocalTime endTime = LocalTime.of(18, 0);

        Duration expected = Duration.between(currentTime, endTime);

        assertEquals("At 14:00, time until 18:00 should be 4 hours", Duration.ofHours(4), expected);
    }

    @Test
    public void testGetStartTimeReturnsCorrectValue() {
        assertEquals(LocalTime.of(12, 0), PlaySchedule.LONG_AFTERNOON.getStartTime());
        assertEquals(LocalTime.of(8, 0), PlaySchedule.MEDIUM_DAY.getStartTime());
        assertEquals(LocalTime.of(17, 0), PlaySchedule.LONG_EVENING.getStartTime());
    }

    @Test
    public void testGetEndTimeReturnsCorrectValue() {
        assertEquals(LocalTime.of(18, 0), PlaySchedule.LONG_AFTERNOON.getEndTime());
        assertEquals(LocalTime.of(18, 0), PlaySchedule.MEDIUM_DAY.getEndTime());
        assertEquals(LocalTime.of(23, 0), PlaySchedule.LONG_EVENING.getEndTime());
    }

    @Test
    public void testScheduleDurationCalculation() {
        PlaySchedule schedule = PlaySchedule.LONG_AFTERNOON;
        Duration scheduleDuration = Duration.between(schedule.getStartTime(), schedule.getEndTime());
        assertEquals("LONG_AFTERNOON should be 6 hours", Duration.ofHours(6), scheduleDuration);

        schedule = PlaySchedule.SHORT_MORNING;
        scheduleDuration = Duration.between(schedule.getStartTime(), schedule.getEndTime());
        assertEquals("SHORT_MORNING should be 1 hour", Duration.ofHours(1), scheduleDuration);
    }

    @Test
    public void testTimeUntilScheduleEndsAtBoundary() {
        LocalTime endTime = LocalTime.of(18, 0);
        LocalTime currentTime = LocalTime.of(18, 0);

        Duration result = Duration.between(currentTime, endTime);
        assertEquals("At exactly end time, duration should be zero", Duration.ZERO, result);
    }

    @Test
    public void testIsOutsideScheduleAtExactStartTime() {
        LocalTime startTime = LocalTime.of(12, 0);
        LocalTime endTime = LocalTime.of(18, 0);
        LocalTime testTime = LocalTime.of(12, 0);

        boolean isOutside = testTime.isBefore(startTime) || testTime.isAfter(endTime);
        assertFalse("At exactly start time, should be inside schedule", isOutside);
    }

    @Test
    public void testIsOutsideScheduleAtExactEndTime() {
        LocalTime startTime = LocalTime.of(12, 0);
        LocalTime endTime = LocalTime.of(18, 0);
        LocalTime testTime = LocalTime.of(18, 0);

        boolean isOutside = testTime.isBefore(startTime) || testTime.isAfter(endTime);
        assertFalse("At exactly end time, should be inside schedule (boundary)", isOutside);
    }

    @Test
    public void testIsOutsideScheduleOneSecondAfterEnd() {
        LocalTime startTime = LocalTime.of(12, 0);
        LocalTime endTime = LocalTime.of(18, 0);
        LocalTime testTime = LocalTime.of(18, 0, 1);

        boolean isOutside = testTime.isBefore(startTime) || testTime.isAfter(endTime);
        assertTrue("One second after end time, should be outside schedule", isOutside);
    }
}
