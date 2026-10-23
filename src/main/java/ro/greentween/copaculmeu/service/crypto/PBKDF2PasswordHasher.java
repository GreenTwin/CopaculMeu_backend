package ro.greentween.copaculmeu.service.crypto;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * PBKDF2 hasher with 1024 bytes key size and 10k iterations class used for cryptographic functions
 */
public class PBKDF2PasswordHasher implements PasswordHasher {
    private static final int HASH_BYTE_SIZE = 1024;
    private static final int PBKDF2_ITERATIONS = 10000;
    public static final String VERSION = "V1";

    private static SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a cryptographically secure 512 bytes string that can be used as a password salt
     * @return A 512-bytes String, generated randomly using {@link SecureRandom}
     */
    public String generateSalt() {
        byte[] bytes = new byte[512];
        secureRandom.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Generate a hash for a password
     * @param password The password in clear value
     * @param salt A salt used for hashing
     * @param pepper A pepper that should be used for hashing
     * @return The hashed password
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    @Override
    public String generateHashForPassword(String password, String salt, String pepper) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
        String saltedPassword = (pepper != null && !pepper.isEmpty()) ? password + pepper : password;
        byte[] saltBytes = salt != null ? salt.getBytes(java.nio.charset.StandardCharsets.UTF_8) : new byte[0];
        PBEKeySpec spec = new PBEKeySpec(saltedPassword.toCharArray(), saltBytes, PBKDF2_ITERATIONS, HASH_BYTE_SIZE);
        byte[] hash = secretKeyFactory.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(hash);
    }

    public String hashVersion() {
        return VERSION;
    }
}
