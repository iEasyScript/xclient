package net.runelite.client.plugins.projectx.util.security;

import com.sun.jna.platform.win32.Crypt32Util;
import net.runelite.client.RuneLite;
import net.runelite.client.util.OSType;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encrypts the passwords and bank PINs saved in profiles.json.
 *
 * <p>On Windows the OS holds the key (DPAPI), so a value only decrypts under the
 * Windows login that saved it. Elsewhere a random key is generated once and kept in
 * {@code ~/.runelite/.profile-key}, readable by the owner only.
 *
 * <p>The scheme this replaces used a key written into the source, so anyone with the
 * source could read a leaked profiles.json. Values in that format still decrypt, and
 * {@link #encrypt} re-encrypts them.
 */
public class Encryption {
    private static final String DPAPI = "dpapi:";
    private static final String LOCAL = "local:";
    private static final byte[] LEGACY_KEY = "projectx12345678".getBytes();
    private static final File LOCAL_KEY_FILE = new File(RuneLite.RUNELITE_DIR, ".profile-key");
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Encrypts {@code plainText}. A value this class already produced comes back
     * unchanged, because the profile panel saves the stored value again when it is
     * not edited; a legacy value is re-encrypted.
     */
    public static String encrypt(String plainText) throws Exception {
        if (isCurrent(plainText)) return plainText;
        if (isLegacy(plainText)) plainText = decryptLegacy(plainText);

        byte[] bytes = plainText.getBytes(StandardCharsets.UTF_8);
        if (OSType.getOSType() == OSType.Windows) {
            return DPAPI + Base64.getEncoder().encodeToString(Crypt32Util.cryptProtectData(bytes));
        }

        byte[] iv = new byte[GCM_IV_BYTES];
        RANDOM.nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(localKey(), "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] sealed = cipher.doFinal(bytes);

        byte[] out = new byte[iv.length + sealed.length];
        System.arraycopy(iv, 0, out, 0, iv.length);
        System.arraycopy(sealed, 0, out, iv.length, sealed.length);
        return LOCAL + Base64.getEncoder().encodeToString(out);
    }

    public static String decrypt(String encryptedText) throws Exception {
        if (encryptedText.startsWith(DPAPI)) {
            byte[] sealed = Base64.getDecoder().decode(encryptedText.substring(DPAPI.length()));
            return new String(Crypt32Util.cryptUnprotectData(sealed), StandardCharsets.UTF_8);
        }
        if (encryptedText.startsWith(LOCAL)) {
            byte[] in = Base64.getDecoder().decode(encryptedText.substring(LOCAL.length()));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(localKey(), "AES"), new GCMParameterSpec(GCM_TAG_BITS, in, 0, GCM_IV_BYTES));
            return new String(cipher.doFinal(in, GCM_IV_BYTES, in.length - GCM_IV_BYTES), StandardCharsets.UTF_8);
        }
        return decryptLegacy(encryptedText);
    }

    /** Whether {@code text} is in the old hard-coded-key format and should be re-encrypted. */
    public static boolean isLegacy(String text) {
        if (text == null || text.isEmpty() || isCurrent(text)) return false;
        try {
            decryptLegacy(text);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isCurrent(String text) {
        return text.startsWith(DPAPI) || text.startsWith(LOCAL);
    }

    // The old format: AES/ECB/PKCS5 under LEGACY_KEY, in the platform charset.
    private static String decryptLegacy(String encryptedText) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(LEGACY_KEY, "AES"));
        return new String(cipher.doFinal(Base64.getDecoder().decode(encryptedText)));
    }

    private static synchronized byte[] localKey() throws IOException {
        Path keyFile = LOCAL_KEY_FILE.toPath();
        if (!Files.exists(keyFile)) {
            byte[] key = new byte[32];
            RANDOM.nextBytes(key);
            Files.createDirectories(keyFile.getParent());
            Path tmp = Files.createTempFile(keyFile.getParent(), ".profile-key", ".tmp",
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            Files.write(tmp, key);
            try {
                // Two clients starting at once: whichever moves first wins, the other reads its key
                Files.move(tmp, keyFile);
            } catch (FileAlreadyExistsException e) {
                Files.delete(tmp);
            }
        }
        return Files.readAllBytes(keyFile);
    }
}
