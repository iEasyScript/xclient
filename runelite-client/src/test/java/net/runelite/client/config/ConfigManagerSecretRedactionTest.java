package net.runelite.client.config;

import java.util.Collections;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ConfigManagerSecretRedactionTest
{
	private static final Set<String> NONE = Collections.emptySet();

	@Test
	public void declaredSecretKeyIsRedacted()
	{
		Set<String> declared = Collections.singleton("twitch.oauth");

		assertTrue(ConfigManager.isSecretKey(declared, "twitch", "oauth"));
		assertEquals(ConfigManager.REDACTED, ConfigManager.redactForLog(declared, "twitch", "oauth", "abc123"));
		assertFalse(ConfigManager.isSecretKey(declared, "other", "oauth"));
	}

	@Test
	public void sensitiveKeyNamesAreRedactedWithoutDeclaration()
	{
		String[] keys = {
			"password", "accountPassword", "alertLinkToken", "agentToken", "TOKEN", "clientSecret",
			"apiKey", "api_key", "API-KEY", "discordWebhookUrl", "webhook", "bankPin", "pin", "PIN",
			"pin_code", "pinCode", "bankpin", "recoveryPins"
		};
		for (String key : keys)
		{
			assertTrue(key, ConfigManager.isSecretKey(NONE, "group", key));
			assertEquals(key, ConfigManager.REDACTED, ConfigManager.redactForLog(NONE, "group", key, "value"));
		}
	}

	@Test
	public void ordinaryKeysAreLoggedAsIs()
	{
		String[] keys = {
			"pinnedPlugins", "spinningWheel", "pineappleCount", "opinion", "enabled", "keyPinned", "hotkey", "apiUrl",
			"showOverlay"
		};
		for (String key : keys)
		{
			assertFalse(key, ConfigManager.isSecretKey(NONE, "group", key));
			assertEquals(key, "value", ConfigManager.redactForLog(NONE, "group", key, "value"));
		}
	}

	@Test
	public void nullValueAndNullKeyStayUnredacted()
	{
		assertNull(ConfigManager.redactForLog(NONE, "group", "password", null));
		assertFalse(ConfigManager.isSecretKey(NONE, "group", null));
		assertEquals(42, ConfigManager.redactForLog(NONE, "group", null, 42));
	}
}
