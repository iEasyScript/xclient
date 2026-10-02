package net.runelite.client.plugins.projectx.util.antiban;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The split between the user's settings and the live ones scripts write. No ConfigManager exists in a unit
 * test, so loading finds no saved profile and the user's settings start as the defaults.
 */
public class Rs2AntibanSettingsUserLayerTest {

	@Before
	public void startFromDefaults() {
		Rs2AntibanSettings.resetEverything();
		Rs2AntibanSettings.loadFromProfile();
	}

	@After
	public void cleanUp() {
		Rs2AntibanSettings.resetEverything();
		Rs2AntibanSettings.loadFromProfile();
	}

	@Test
	public void scriptWritesAreSeenAsScriptControl() {
		assertFalse(Rs2AntibanSettings.isScriptControlled());
		Rs2AntibanSettings.usePlayStyle = true;
		assertTrue(Rs2AntibanSettings.isScriptControlled());

		Rs2AntibanSettings.restoreUserSettings();
		assertFalse(Rs2AntibanSettings.usePlayStyle);
		assertFalse(Rs2AntibanSettings.isScriptControlled());
	}

	@Test
	public void userChangeDuringScriptControlDoesNotAdoptTheScriptsValues() {
		// A script's template is live.
		Rs2AntibanSettings.usePlayStyle = true;
		Rs2AntibanSettings.actionCooldownChance = 0.9;

		Rs2AntibanSettings.userChange(() -> Rs2AntibanSettings.devDebug = true);

		// The user's one change is live on top of the script's values...
		assertTrue(Rs2AntibanSettings.devDebug);
		assertTrue(Rs2AntibanSettings.usePlayStyle);
		assertEquals(0.9, Rs2AntibanSettings.actionCooldownChance, 1e-9);

		// ...and once the script's values go, only the user's change remains.
		Rs2AntibanSettings.restoreUserSettings();
		assertTrue(Rs2AntibanSettings.devDebug);
		assertFalse(Rs2AntibanSettings.usePlayStyle);
		assertEquals(0.1, Rs2AntibanSettings.actionCooldownChance, 1e-9);
	}

	@Test
	public void overrideUndoesScriptWrites() {
		Rs2AntibanSettings.userChange(() -> Rs2AntibanSettings.overwriteScriptSettings = true);
		assertTrue(Rs2AntibanSettings.isOverriding());

		Rs2AntibanSettings.usePlayStyle = true;
		Rs2AntibanSettings.enforceUserSettingsIfOverriding();
		assertFalse(Rs2AntibanSettings.usePlayStyle);
	}

	@Test
	public void scriptCannotSwitchTheOverrideOff() {
		Rs2AntibanSettings.userChange(() -> Rs2AntibanSettings.overwriteScriptSettings = true);

		Rs2AntibanSettings.overwriteScriptSettings = false;
		Rs2AntibanSettings.usePlayStyle = true;
		assertTrue(Rs2AntibanSettings.isOverriding());

		Rs2AntibanSettings.enforceUserSettingsIfOverriding();
		assertTrue(Rs2AntibanSettings.overwriteScriptSettings);
		assertFalse(Rs2AntibanSettings.usePlayStyle);
	}

	@Test
	public void scriptResetLeavesTheOverrideAlone() {
		Rs2AntibanSettings.userChange(() -> Rs2AntibanSettings.overwriteScriptSettings = true);
		Rs2AntibanSettings.reset();
		assertTrue(Rs2AntibanSettings.overwriteScriptSettings);
	}

	@Test
	public void userChangeIsNotUndoneByEnforcementInsideIt() {
		Rs2AntibanSettings.userChange(() -> Rs2AntibanSettings.overwriteScriptSettings = true);

		// A preset that sets an activity enforces the override part-way through applying itself.
		Rs2AntibanSettings.userChange(() -> {
			Rs2AntibanSettings.usePlayStyle = true;
			Rs2AntibanSettings.enforceUserSettingsIfOverriding();
			Rs2AntibanSettings.simulateFatigue = true;
		});

		assertTrue(Rs2AntibanSettings.usePlayStyle);
		assertTrue(Rs2AntibanSettings.simulateFatigue);
		assertFalse(Rs2AntibanSettings.isScriptControlled());
	}

	@Test
	public void resetEverythingClearsTheOverrideToo() {
		Rs2AntibanSettings.userChange(() -> Rs2AntibanSettings.overwriteScriptSettings = true);
		Rs2AntibanSettings.userChange(Rs2AntibanSettings::resetEverything);
		assertFalse(Rs2AntibanSettings.overwriteScriptSettings);
		assertFalse(Rs2AntibanSettings.isOverriding());
	}

	@Test
	public void presetsApplyUnderOverride() {
		Rs2AntibanSettings.userChange(() -> Rs2AntibanSettings.overwriteScriptSettings = true);
		Rs2AntibanSettings.userChange(AntibanPreset.CAUTIOUS::apply);

		assertTrue(Rs2AntibanSettings.timeOfDayAdjust);
		assertEquals(0.8, Rs2AntibanSettings.actionCooldownChance, 1e-9);
		assertTrue("the preset must not touch the override", Rs2AntibanSettings.overwriteScriptSettings);
	}
}
