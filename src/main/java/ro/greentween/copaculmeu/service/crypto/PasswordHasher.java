package ro.greentween.copaculmeu.service.crypto;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public interface PasswordHasher {

    /**
     * Generates a cryptographically secure 512 bytes string that can be used as a password salt
     * @return
     */
    String generateSalt();

    /**
     * Generate a hash for a password
     * @param password The password in clear value
     * @param salt A salt used for hashing
     * @param pepper A pepper that should be used for hashing
     * @return The hashed password
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    String generateHashForPassword(String password, String salt, String pepper) throws NoSuchAlgorithmException, InvalidKeySpecException;

    String hashVersion();
}
