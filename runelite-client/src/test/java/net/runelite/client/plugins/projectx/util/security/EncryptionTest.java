package net.runelite.client.plugins.projectx.util.security;

import org.junit.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Saved passwords and bank PINs live in profiles.json. Upgrading the scheme must not
 * lose anyone's saved login, and re-saving an unedited field must not encrypt twice.
 */
public class EncryptionTest
{
	@Test
	public void roundTrips()
	{
		for (String secret : new String[]{"hunter2", "1234", "a password that is well over sixteen characters", "ends with ==", "pässwörd"})
		{
			assertEquals(secret, decrypt(encrypt(secret)));
		}
	}

	@Test
	public void newValuesDoNotUseTheHardCodedKey()
	{
		String sealed = encrypt("hunter2");
		assertFalse(Encryption.isLegacy(sealed));
		assertNotEquals(legacyEncrypt("hunter2"), sealed);
	}

	@Test
	public void reEncryptingAnEncryptedValueChangesNothing()
	{
		// The profile panel shows the stored value and saves it again when it is not edited
		String sealed = encrypt("a password that is well over sixteen characters");
		assertEquals(sealed, encrypt(sealed));
	}

	@Test
	public void legacyValuesStillDecrypt()
	{
		for (String secret : new String[]{"hunter2", "1234", "a password that is well over sixteen characters"})
		{
			String legacy = legacyEncrypt(secret);
			assertTrue(Encryption.isLegacy(legacy));
			assertEquals(secret, decrypt(legacy));
		}
	}

	@Test
	public void legacyValuesAreUpgradedNotDoubleEncrypted()
	{
		String upgraded = encrypt(legacyEncrypt("hunter2"));
		assertFalse(Encryption.isLegacy(upgraded));
		assertEquals("hunter2", decrypt(upgraded));
	}

	@Test
	public void plainTextIsNotMistakenForLegacy()
	{
		assertFalse(Encryption.isLegacy(null));
		assertFalse(Encryption.isLegacy(""));
		assertFalse(Encryption.isLegacy("hunter2"));
		assertFalse(Encryption.isLegacy("1234"));
	}

	private static String encrypt(String plainText)
	{
		try
		{
			return Encryption.encrypt(plainText);
		}
		catch (Exception e)
		{
			throw new AssertionError(e);
		}
	}

	private static String decrypt(String encryptedText)
	{
		try
		{
			return Encryption.decrypt(encryptedText);
		}
		catch (Exception e)
		{
			throw new AssertionError(e);
		}
	}

	/** What the previous implementation wrote. */
	private static String legacyEncrypt(String plainText)
	{
		try
		{
			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec("projectx12345678".getBytes(), "AES"));
			return Base64.getEncoder().encodeToString(cipher.doFinal(plainText.getBytes()));
		}
		catch (Exception e)
		{
			throw new AssertionError(e);
		}
	}
}
