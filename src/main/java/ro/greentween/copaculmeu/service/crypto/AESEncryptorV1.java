package ro.greentween.copaculmeu.service.crypto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

@Slf4j
@Component
public class AESEncryptorV1 implements CodeEncryptor {
    public static final String VERSION = "AES_V1";

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TAG_LENGTH_BIT = 128; // Recommended for GCM
    private static final int IV_LENGTH_BYTE = 12; // Recommended for GCM

    @Override
    public String getVersion() {
        return VERSION;
    }

    /**
     * Generates a strong secret key for AES encryption.
     *
     * @return A SecretKey object.
     */
    private SecretKey generateKey(String keyString) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha.digest(keyString.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }


    /**
     * Encrypts a given string using AES/GCM/NoPadding.
     *
     * @param plainText The string to encrypt.
     * @param keyString The encryption key.
     * @return The Base64 encoded encrypted string, or null on failure
     */
    @Override
    public String encrypt(String plainText, String keyString) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKey key = generateKey(keyString);

            byte[] iv = generateIV();
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);

            cipher.init(Cipher.ENCRYPT_MODE, key, gcmParameterSpec);
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            //Prepend IV to cipherText
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);


            return Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException e) {
            log.error("Error while encrypting: {}", e.getMessage());
            throw e;
        }
    }


    /**
     * Decrypts a given Base64 encoded string using AES/GCM/NoPadding.
     *
     * @param base64CipherText The Base64 encoded string to decrypt.
     * @param keyString The decryption key.
     * @return The decrypted string, or null on failure.
     */
    @Override
    public String decrypt(String base64CipherText, String keyString) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        try {
            byte[] combined = Base64.getDecoder().decode(base64CipherText);

            byte[] iv = new byte[IV_LENGTH_BYTE];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTE);
            byte[] cipherText = new byte[combined.length - IV_LENGTH_BYTE];
            System.arraycopy(combined, IV_LENGTH_BYTE, cipherText, 0, combined.length - IV_LENGTH_BYTE);

            SecretKey key = generateKey(keyString);
            Cipher cipher = Cipher.getInstance(ALGORITHM);

            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmParameterSpec);
            byte[] plainText = cipher.doFinal(cipherText);

            return new String(plainText, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            log.error("Error while decrypting: {}", e.getMessage());
            throw e;
        }
    }

    private byte[] generateIV() {
        byte[] iv = new byte[IV_LENGTH_BYTE];
        SECURE_RANDOM.nextBytes(iv);
        return iv;
    }
}