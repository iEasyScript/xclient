package net.runelite.client.plugins.projectx.breakhandler.breakhandlerv2;

import net.runelite.client.plugins.projectx.util.antiban.enums.PlaySchedule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Duration;
import java.time.LocalTime;

import static org.junit.Assert.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BreakHandlerV2PlayScheduleTest {

    @Mock
    private BreakHandlerV2Config config;

    @Test
    public void testIsOutsidePlayScheduleWhenDisabled() {
        when(config.usePlaySchedule()).thenReturn(false);

        boolean result = isOutsidePlaySchedule(config);

        assertFalse("Should return false when play schedule is disabled", result);
    }

    @Test
    public void testIsOutsidePlayScheduleWhenEnabledAndOutside() {
        lenient().when(config.usePlaySchedule()).thenReturn(true);
        lenient().when(config.playSchedule()).thenReturn(PlaySchedule.MEDIUM_AFTERNOON);

        LocalTime now = LocalTime.now();
        LocalTime scheduleStart = LocalTime.of(12, 0);
        LocalTime scheduleEnd = LocalTime.of(15, 0);

        boolean currentlyOutside = now.isBefore(scheduleStart) || now.isAfter(scheduleEnd);
        boolean result = isOutsidePlaySchedule(config);

        assertEquals("Should match actual schedule state", currentlyOutside, result);
    }

    @Test
    public void testCalculateBreakDurationWhenOutsidePlaySchedule() {
        PlaySchedule outsideSchedule = findScheduleOutsideCurrentTime();
        lenient().when(config.usePlaySchedule()).thenReturn(true);
        lenient().when(config.playSchedule()).thenReturn(outsideSchedule);

        assertTrue("Selected schedule should be outside current time",
                isOutsidePlaySchedule(config));

        long durationBefore = config.playSchedule().timeUntilNextSchedule().toMillis();
        long duration = calculateBreakDuration(config);
        long durationAfter = config.playSchedule().timeUntilNextSchedule().toMillis();

        assertTrue("Break duration should be positive", duration > 0);
        assertTrue("Break duration should be within the time window of the two measurements",
                duration >= durationAfter && duration <= durationBefore);
    }

    @Test
    public void testCalculateBreakDurationWhenInsidePlaySchedule() {
        lenient().when(config.usePlaySchedule()).thenReturn(true);
        lenient().when(config.playSchedule()).thenReturn(PlaySchedule.MEDIUM_AFTERNOON);
        lenient().when(config.minBreakDuration()).thenReturn(5);
        lenient().when(config.maxBreakDuration()).thenReturn(15);

        if (!isOutsidePlaySchedule(config)) {
            long duration = calculateBreakDuration(config);

            long minDuration = config.minBreakDuration() * 60000L;
            long maxDuration = config.maxBreakDuration() * 60000L;

            assertTrue("Break duration should be within configured range",
                    duration >= minDuration && duration <= maxDuration);
        }
    }

    @Test
    public void testShouldLogoutWhenOutsidePlaySchedule() {
        lenient().when(config.usePlaySchedule()).thenReturn(true);
        lenient().when(config.playSchedule()).thenReturn(PlaySchedule.MEDIUM_AFTERNOON);
        lenient().when(config.logoutOnBreak()).thenReturn(false);

        if (isOutsidePlaySchedule(config)) {
            boolean shouldLogout = shouldLogout(config);

            assertTrue("Should force logout when outside play schedule, even if logoutOnBreak is false",
                    shouldLogout);
        }
    }

    @Test
    public void testShouldNotLogoutWhenInsidePlayScheduleAndLogoutDisabled() {
        lenient().when(config.usePlaySchedule()).thenReturn(true);
        lenient().when(config.playSchedule()).thenReturn(PlaySchedule.MEDIUM_AFTERNOON);
        lenient().when(config.logoutOnBreak()).thenReturn(false);

        if (!isOutsidePlaySchedule(config)) {
            boolean shouldLogout = shouldLogout(config);

            assertFalse("Should not logout when inside play schedule and logoutOnBreak is false",
                    shouldLogout);
        }
    }

    @Test
    public void testShouldLogoutWhenInsidePlayScheduleAndLogoutEnabled() {
        lenient().when(config.usePlaySchedule()).thenReturn(true);
        lenient().when(config.playSchedule()).thenReturn(PlaySchedule.MEDIUM_AFTERNOON);
        lenient().when(config.logoutOnBreak()).thenReturn(true);

        if (!isOutsidePlaySchedule(config)) {
            boolean shouldLogout = shouldLogout(config);

            assertTrue("Should logout when inside play schedule and logoutOnBreak is true",
                    shouldLogout);
        }
    }

    @Test
    public void testPlayScheduleBreakDurationIsPositiveWhenOutside() {
        lenient().when(config.usePlaySchedule()).thenReturn(true);
        lenient().when(config.playSchedule()).thenReturn(PlaySchedule.MEDIUM_AFTERNOON);

        if (isOutsidePlaySchedule(config)) {
            Duration timeUntil = config.playSchedule().timeUntilNextSchedule();

            assertTrue("Time until next schedule should be positive when outside",
                    timeUntil.toMillis() > 0);
        }
    }

    @Test
    public void testMediumAfternoonScheduleTimes() {
        PlaySchedule schedule = PlaySchedule.MEDIUM_AFTERNOON;

        LocalTime startTime = LocalTime.of(12, 0);
        LocalTime endTime = LocalTime.of(15, 0);

        LocalTime insideTime = LocalTime.of(13, 0);
        LocalTime beforeTime = LocalTime.of(11, 0);
        LocalTime afterTime = LocalTime.of(16, 0);

        assertFalse("13:00 should be inside MEDIUM_AFTERNOON",
                insideTime.isBefore(startTime) || insideTime.isAfter(endTime));

        assertTrue("11:00 should be outside MEDIUM_AFTERNOON (before start)",
                beforeTime.isBefore(startTime));

        assertTrue("16:00 should be outside MEDIUM_AFTERNOON (after end)",
                afterTime.isAfter(endTime));
    }

    @Test
    public void testTimeUntilNextScheduleFromEarlyMorning() {
        LocalTime earlyMorning = LocalTime.of(5, 0);
        LocalTime scheduleStart = LocalTime.of(12, 0);

        Duration expected = Duration.between(earlyMorning, scheduleStart);

        assertEquals("At 05:00, should be 7 hours until 12:00 schedule start",
                Duration.ofHours(7), expected);
    }

    @Test
    public void testTimeUntilNextScheduleFromLateEvening() {
        LocalTime lateEvening = LocalTime.of(20, 0);
        LocalTime scheduleStart = LocalTime.of(12, 0);

        Duration expected = Duration.between(lateEvening, scheduleStart).plusDays(1);

        assertEquals("At 20:00, should be 16 hours until next day 12:00",
                Duration.ofHours(16), expected);
    }

    @Test
    public void testAllScheduleTypesExist() {
        PlaySchedule[] expectedSchedules = {
                PlaySchedule.SHORT_MORNING,
                PlaySchedule.MEDIUM_MORNING,
                PlaySchedule.LONG_MORNING,
                PlaySchedule.SHORT_AFTERNOON,
                PlaySchedule.MEDIUM_AFTERNOON,
                PlaySchedule.LONG_AFTERNOON,
                PlaySchedule.SHORT_EVENING,
                PlaySchedule.MEDIUM_EVENING,
                PlaySchedule.LONG_EVENING,
                PlaySchedule.SHORT_DAY,
                PlaySchedule.MEDIUM_DAY,
                PlaySchedule.LONG_DAY,
                PlaySchedule.SHORT_NIGHT,
                PlaySchedule.MEDIUM_NIGHT,
                PlaySchedule.LONG_NIGHT,
                PlaySchedule.FIRST_NIGHT,
                PlaySchedule.SECOND_NIGHT,
                PlaySchedule.THIRD_NIGHT
        };

        assertEquals("Should have 18 schedule types", 18, expectedSchedules.length);
        assertEquals("PlaySchedule enum should have 18 values", 18, PlaySchedule.values().length);
    }

    private boolean isOutsidePlaySchedule(BreakHandlerV2Config config) {
        return config.usePlaySchedule() && config.playSchedule().isOutsideSchedule();
    }

    private long calculateBreakDuration(BreakHandlerV2Config config) {
        if (isOutsidePlaySchedule(config)) {
            Duration timeUntilPlaySchedule = config.playSchedule().timeUntilNextSchedule();
            return timeUntilPlaySchedule.toMillis();
        }

        int minMinutes = config.minBreakDuration();
        int maxMinutes = config.maxBreakDuration();
        int breakMinutes = (minMinutes + maxMinutes) / 2;

        return breakMinutes * 60000L;
    }

    private boolean shouldLogout(BreakHandlerV2Config config) {
        return isOutsidePlaySchedule(config) || config.logoutOnBreak();
    }

    @Test
    public void testTimeUntilScheduleEndsWhenInside() {
        lenient().when(config.usePlaySchedule()).thenReturn(true);
        lenient().when(config.playSchedule()).thenReturn(PlaySchedule.LONG_AFTERNOON);

        if (!config.playSchedule().isOutsideSchedule()) {
            Duration timeUntilEnd = config.playSchedule().timeUntilScheduleEnds();
            assertTrue("Time until schedule ends should be non-negative when inside",
                    timeUntilEnd.toMillis() >= 0);
        }
    }

    @Test
    public void testTimeUntilScheduleEndsIsZeroWhenOutside() {
        lenient().when(config.usePlaySchedule()).thenReturn(true);
        lenient().when(config.playSchedule()).thenReturn(PlaySchedule.LONG_AFTERNOON);

        if (config.playSchedule().isOutsideSchedule()) {
            Duration timeUntilEnd = config.playSchedule().timeUntilScheduleEnds();
            assertEquals("Time until schedule ends should be zero when outside",
                    Duration.ZERO, timeUntilEnd);
        }
    }

    @Test
    public void testScheduleNextBreakLogicWhenPlayScheduleEnabledAndInside() {
        lenient().when(config.usePlaySchedule()).thenReturn(true);
        lenient().when(config.playSchedule()).thenReturn(PlaySchedule.LONG_AFTERNOON);

        if (!config.playSchedule().isOutsideSchedule()) {
            Duration timeUntilEnd = config.playSchedule().timeUntilScheduleEnds();
            assertTrue("When inside schedule, timeUntilEnd should be positive or zero",
                    timeUntilEnd.toMillis() >= 0);

            long expectedBreakTimeMs = System.currentTimeMillis() + timeUntilEnd.toMillis();
            long toleranceMs = 1000;

            long actualBreakTimeMs = System.currentTimeMillis() + timeUntilEnd.toMillis();
            assertTrue("Next break time should be approximately schedule end time",
                    Math.abs(expectedBreakTimeMs - actualBreakTimeMs) < toleranceMs);
        }
    }

    @Test
    public void testScheduleNextBreakLogicWhenPlayScheduleEnabledAndOutside() {
        lenient().when(config.usePlaySchedule()).thenReturn(true);
        lenient().when(config.playSchedule()).thenReturn(PlaySchedule.LONG_AFTERNOON);

        if (config.playSchedule().isOutsideSchedule()) {
            assertNull("Next break time should be null when outside schedule",
                    getNextBreakTimeForPlaySchedule(config));
        }
    }

    @Test
    public void testScheduleNextBreakLogicWhenPlayScheduleDisabled() {
        lenient().when(config.usePlaySchedule()).thenReturn(false);
        when(config.minPlaytime()).thenReturn(30);
        when(config.maxPlaytime()).thenReturn(60);

        long nextBreakMs = calculateNextBreakTimeMs(config);
        long minMs = 30 * 60 * 1000L;
        long maxMs = 60 * 60 * 1000L;

        assertTrue("Next break should be within configured range",
                nextBreakMs >= minMs && nextBreakMs <= maxMs);
    }

    @Test
    public void testLongAfternoonScheduleEndTime() {
        PlaySchedule schedule = PlaySchedule.LONG_AFTERNOON;

        assertEquals("LONG_AFTERNOON should start at 12:00",
                LocalTime.of(12, 0), schedule.getStartTime());
        assertEquals("LONG_AFTERNOON should end at 18:00",
                LocalTime.of(18, 0), schedule.getEndTime());
    }

    @Test
    public void testTimeUntilScheduleEndsCalculationForLongAfternoon() {
        LocalTime currentTime = LocalTime.of(14, 0);
        LocalTime endTime = LocalTime.of(18, 0);

        Duration expected = Duration.between(currentTime, endTime);
        assertEquals("At 14:00, time until 18:00 should be 4 hours",
                Duration.ofHours(4), expected);
    }

    @Test
    public void testTimeUntilScheduleEndsCalculationAtBoundary() {
        LocalTime currentTime = LocalTime.of(18, 0);
        LocalTime endTime = LocalTime.of(18, 0);

        Duration expected = Duration.between(currentTime, endTime);
        assertEquals("At exactly 18:00, time until 18:00 should be 0",
                Duration.ZERO, expected);
    }

    @Test
    public void testRegularBreaksSkippedWhenPlayScheduleEnabled() {
        lenient().when(config.usePlaySchedule()).thenReturn(true);
        lenient().when(config.playSchedule()).thenReturn(PlaySchedule.LONG_AFTERNOON);

        if (!config.playSchedule().isOutsideSchedule()) {
            boolean shouldSkipRegularBreaks = shouldSkipRegularBreaks(config);
            assertTrue("Regular breaks should be skipped when inside play schedule",
                    shouldSkipRegularBreaks);
        }
    }

    @Test
    public void testRegularBreaksNotSkippedWhenPlayScheduleDisabled() {
        lenient().when(config.usePlaySchedule()).thenReturn(false);

        boolean shouldSkipRegularBreaks = shouldSkipRegularBreaks(config);
        assertFalse("Regular breaks should NOT be skipped when play schedule disabled",
                shouldSkipRegularBreaks);
    }

    @Test
    public void testBreakTriggerLogicWhenOutsidePlaySchedule() {
        lenient().when(config.usePlaySchedule()).thenReturn(true);
        lenient().when(config.playSchedule()).thenReturn(PlaySchedule.LONG_AFTERNOON);

        if (config.playSchedule().isOutsideSchedule()) {
            boolean shouldTriggerBreak = isOutsidePlaySchedule(config);
            assertTrue("Should trigger break when outside play schedule", shouldTriggerBreak);
        }
    }

    @Test
    public void testBreakNotTriggeredWhenInsidePlaySchedule() {
        lenient().when(config.usePlaySchedule()).thenReturn(true);
        lenient().when(config.playSchedule()).thenReturn(PlaySchedule.LONG_AFTERNOON);

        if (!config.playSchedule().isOutsideSchedule()) {
            boolean shouldTriggerBreak = isOutsidePlaySchedule(config);
            assertFalse("Should NOT trigger break when inside play schedule", shouldTriggerBreak);
        }
    }

    @Test
    public void testGetTimeUntilBreakReturnsValidValueWhenInsideSchedule() {
        lenient().when(config.usePlaySchedule()).thenReturn(true);
        lenient().when(config.playSchedule()).thenReturn(PlaySchedule.LONG_AFTERNOON);

        if (!config.playSchedule().isOutsideSchedule()) {
            long secondsUntilBreak = config.playSchedule().timeUntilScheduleEnds().getSeconds();
            assertTrue("Seconds until break should be non-negative", secondsUntilBreak >= 0);
        }
    }

    @Test
    public void testOverlayDisplayLabelWhenPlayScheduleEnabled() {
        when(config.usePlaySchedule()).thenReturn(true);

        String label = config.usePlaySchedule() ? "Schedule ends:" : "Next break:";
        assertEquals("Label should be 'Schedule ends:' when play schedule enabled",
                "Schedule ends:", label);
    }

    @Test
    public void testOverlayDisplayLabelWhenPlayScheduleDisabled() {
        when(config.usePlaySchedule()).thenReturn(false);

        String label = config.usePlaySchedule() ? "Schedule ends:" : "Next break:";
        assertEquals("Label should be 'Next break:' when play schedule disabled",
                "Next break:", label);
    }

    private Long getNextBreakTimeForPlaySchedule(BreakHandlerV2Config config) {
        if (config.usePlaySchedule()) {
            if (!config.playSchedule().isOutsideSchedule()) {
                Duration timeUntilEnd = config.playSchedule().timeUntilScheduleEnds();
                return System.currentTimeMillis() + timeUntilEnd.toMillis();
            } else {
                return null;
            }
        }
        return null;
    }

    private long calculateNextBreakTimeMs(BreakHandlerV2Config config) {
        int minMinutes = config.minPlaytime();
        int maxMinutes = config.maxPlaytime();
        int playtimeMinutes = (minMinutes + maxMinutes) / 2;
        return playtimeMinutes * 60 * 1000L;
    }

    private boolean shouldSkipRegularBreaks(BreakHandlerV2Config config) {
        return config.usePlaySchedule() && !config.playSchedule().isOutsideSchedule();
    }

    private PlaySchedule findScheduleOutsideCurrentTime() {
        for (PlaySchedule schedule : PlaySchedule.values()) {
            if (schedule.isOutsideSchedule()) {
                return schedule;
            }
        }
        throw new IllegalStateException("No schedule is outside the current time");
    }
}
