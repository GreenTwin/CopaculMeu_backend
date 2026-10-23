package ro.greentween.copaculmeu.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.greentween.copaculmeu.data.dao.UserActivationCodeDao;
import ro.greentween.copaculmeu.data.dao.UserDao;
import ro.greentween.copaculmeu.data.dto.UserRegisterRequest;
import ro.greentween.copaculmeu.data.dto.UserResponse;
import ro.greentween.copaculmeu.data.enums.UserRole;
import ro.greentween.copaculmeu.data.enums.UserStatus;
import ro.greentween.copaculmeu.data.model.User;
import ro.greentween.copaculmeu.data.model.UserActivationCode;
import ro.greentween.copaculmeu.service.crypto.CodeEncryptor;
import ro.greentween.copaculmeu.service.crypto.CodeEncryptorFactory;
import ro.greentween.copaculmeu.service.crypto.PasswordHasher;
import ro.greentween.copaculmeu.service.crypto.PasswordHasherFactory;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserDao userDao;
    private final UserActivationCodeDao activationCodeDao;
    private final CodeEncryptorFactory codeEncryptorFactory;

    @Value("${app.security.pepper:CopaculMeuPepper2026!#$TreeApp}")
    private String pepper;

    @Value("${app.security.activation-code-secret:CopaculMeuActivationCodeSecretKey2026!}")
    private String activationSecret;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Transactional
    public UserResponse registerUser(UserRegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userDao.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("User with email " + normalizedEmail + " already exists");
        }

        PasswordHasher hasher = PasswordHasherFactory.getHasher();
        String salt = hasher.generateSalt();

        try {
            String hash = hasher.generateHashForPassword(request.getPassword(), salt, pepper);

            User user = User.builder()
                    .email(normalizedEmail)
                    .name(request.getName() != null ? request.getName().trim() : null)
                    .passwordHash(hash)
                    .salt(salt)
                    .hashVersion(hasher.hashVersion())
                    .role(UserRole.USER)
                    .status(UserStatus.UNCONFIRMED)
                    .build();

            User savedUser = userDao.persist(user);

            // Generate raw activation code and encrypt it using current CodeEncryptor
            String rawActivationCode = generateActivationCode();
            CodeEncryptor codeEncryptor = codeEncryptorFactory.getEncryptor();
            String encryptedCode = codeEncryptor.encrypt(rawActivationCode, activationSecret);

            UserActivationCode activationCode = UserActivationCode.builder()
                    .user(savedUser)
                    .code(encryptedCode)
                    .encryptorVersion(codeEncryptor.getVersion())
                    .expiresAt(OffsetDateTime.now().plusHours(24))
                    .build();

            activationCodeDao.persist(activationCode);
            log.info("Registered user {} with activation code encrypted using version {}",
                    savedUser.getEmail(), codeEncryptor.getVersion());

            return UserResponse.fromEntity(savedUser);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Error generating password hash", e);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Error encrypting activation code", e);
        }
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        User user = userDao.find(id);
        if (user == null) {
            throw new IllegalArgumentException("User with id " + id + " not found");
        }
        return UserResponse.fromEntity(user);
    }

    private String generateActivationCode() {
        int code = 100_000 + SECURE_RANDOM.nextInt(900_000);
        return String.valueOf(code);
    }
}
