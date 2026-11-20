package ro.greentween.copaculmeu.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.greentween.copaculmeu.data.dao.UserActivationCodeDao;
import ro.greentween.copaculmeu.data.dao.UserDao;
import ro.greentween.copaculmeu.data.dto.*;
import ro.greentween.copaculmeu.data.enums.UserRole;
import ro.greentween.copaculmeu.data.enums.UserStatus;
import ro.greentween.copaculmeu.data.filter.UserFilter;
import ro.greentween.copaculmeu.data.model.User;
import ro.greentween.copaculmeu.data.model.UserActivationCode;
import ro.greentween.copaculmeu.exception.ResourceNotFoundException;
import ro.greentween.copaculmeu.service.crypto.CodeEncryptor;
import ro.greentween.copaculmeu.service.crypto.CodeEncryptorFactory;
import ro.greentween.copaculmeu.service.crypto.PasswordHasher;
import ro.greentween.copaculmeu.service.crypto.PasswordHasherFactory;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserDao userDao;
    private final UserActivationCodeDao activationCodeDao;
    private final CodeEncryptorFactory codeEncryptorFactory;
    private final EmailService emailService;

    @Value("${app.security.pepper}")
    private String pepper;

    @Value("${app.security.activation-code-secret}")
    private String activationSecret;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    public static final int MAX_CODE_ATTEMPTS = 10;

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

            emailService.sendRegistrationConfirmation(savedUser, rawActivationCode);

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

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public UserResponse activateUser(UserActivationRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        User user = userDao.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("User with email " + normalizedEmail + " not found"));

        if (user.getStatus() == UserStatus.ACTIVE) {
            log.info("User {} is already active", normalizedEmail);
            return UserResponse.fromEntity(user);
        }

        if (user.getStatus() == UserStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException("Account is waiting approval from an administrator");
        }

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new IllegalArgumentException("Account is suspended");
        }

        List<UserActivationCode> codes = activationCodeDao.findByUserId(user.getId());
        OffsetDateTime now = OffsetDateTime.now();

        boolean hasExhaustedCode = codes.stream()
                .anyMatch(c -> c.getAttemptsCount() >= MAX_CODE_ATTEMPTS && c.getUsedAt() == null);

        List<UserActivationCode> activeCodes = codes.stream()
                .filter(c -> c.getUsedAt() == null && c.getExpiresAt().isAfter(now) && c.getAttemptsCount() < MAX_CODE_ATTEMPTS)
                .toList();

        if (activeCodes.isEmpty()) {
            if (hasExhaustedCode) {
                throw new IllegalArgumentException("Maximum verification attempts (10) exceeded. Please request a new code.");
            }
            throw new IllegalArgumentException("Invalid or expired activation code");
        }

        UserActivationCode matchedCode = null;
        UserActivationCode activeCodeToIncrement = null;

        for (UserActivationCode activationCode : activeCodes) {
            activeCodeToIncrement = activationCode;

            try {
                CodeEncryptor codeEncryptor = codeEncryptorFactory.getEncryptor(activationCode.getEncryptorVersion());
                String decryptedCode = codeEncryptor.decrypt(activationCode.getCode(), activationSecret);
                if (decryptedCode.equals(request.getCode().trim())) {
                    matchedCode = activationCode;
                    break;
                }
            } catch (GeneralSecurityException e) {
                log.error("Failed to decrypt activation code for user {}", normalizedEmail, e);
            }
        }

        if (matchedCode == null) {
            if (activeCodeToIncrement != null) {
                int newAttempts = activeCodeToIncrement.getAttemptsCount() + 1;
                activeCodeToIncrement.setAttempts(newAttempts);
                if (newAttempts >= MAX_CODE_ATTEMPTS) {
                    activeCodeToIncrement.setExpiresAt(now);
                    activationCodeDao.update(activeCodeToIncrement);
                    throw new IllegalArgumentException("Maximum verification attempts (10) exceeded. Please request a new code.");
                }
                activationCodeDao.update(activeCodeToIncrement);
            }
            throw new IllegalArgumentException("Invalid or expired activation code");
        }

        matchedCode.setAttempts(matchedCode.getAttemptsCount() + 1);
        matchedCode.setUsedAt(now);
        activationCodeDao.update(matchedCode);

        user.setStatus(UserStatus.PENDING_APPROVAL);
        User updatedUser = userDao.update(user);

        log.info("User {} activated successfully", normalizedEmail);
        return UserResponse.fromEntity(updatedUser);
    }

    @Transactional
    public UserResponse setUserStatus(UUID userId, UserStatus status) {
        User user = userDao.find(userId);
        if (user == null) {
            throw new IllegalArgumentException("User with id " + userId + " not found");
        }
        user.setStatus(status);
        User updated = userDao.update(user);
        return UserResponse.fromEntity(updated);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Integer page, Integer size, Integer limit, Integer offset, UserStatus status,
            UserRole role) {
        int effectiveSize = (limit != null && limit > 0) ? limit : (size != null && size > 0 ? size : 20);
        int effectivePage;
        if (offset != null && offset >= 0) {
            effectivePage = offset / effectiveSize;
        } else if (page != null && page >= 0) {
            effectivePage = page;
        } else {
            effectivePage = 0;
        }

        UserFilter filter = new UserFilter();
        if (status != null) {
            filter.setStatus(status);
        }
        if (role != null) {
            filter.setRole(role);
        }

        long totalElements = userDao.count(filter);

        int effectiveOffset = (offset != null && offset >= 0) ? offset : (effectivePage * effectiveSize);
        filter.setOffset(effectiveOffset);
        filter.setLimit(effectiveSize);
        filter.orderByDesc("createdAt");

        List<User> users = userDao.get(filter);
        List<UserResponse> responses = users.stream()
                .map(UserResponse::fromEntity)
                .toList();

        Pageable pageable = PageRequest.of(effectivePage, effectiveSize);
        return new PagedResponse<>(responses, pageable, totalElements);
    }

    @Transactional
    public UserResponse approveUser(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        User user = userDao.find(id);
        if (user == null) {
            throw new ResourceNotFoundException("User with id " + id + " not found");
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new IllegalStateException("Cannot approve a suspended user account");
        }
        if (user.getStatus() != UserStatus.UNCONFIRMED && user.getStatus() != UserStatus.PENDING_APPROVAL) {
            throw new IllegalStateException(
                    "Only unconfirmed user accounts can be approved. Current status: " + user.getStatus());
        }
        user.setStatus(UserStatus.ACTIVE);
        User updated = userDao.update(user);
        log.info("User {} approved successfully by administrator", updated.getEmail());
        return UserResponse.fromEntity(updated);
    }

    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        java.util.Optional<User> optionalUser = userDao.findByEmail(normalizedEmail);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.getStatus() != UserStatus.SUSPENDED) {
                String rawResetCode = generateActivationCode();
                try {
                    CodeEncryptor codeEncryptor = codeEncryptorFactory.getEncryptor();
                    String encryptedCode = codeEncryptor.encrypt(rawResetCode, activationSecret);

                    UserActivationCode resetCode = UserActivationCode.builder()
                            .user(user)
                            .code(encryptedCode)
                            .encryptorVersion(codeEncryptor.getVersion())
                            .expiresAt(OffsetDateTime.now().plusHours(24))
                            .build();

                    activationCodeDao.persist(resetCode);
                    emailService.sendForgotPassword(user, rawResetCode);
                    log.info("Dispatched password reset code for user {}", normalizedEmail);
                } catch (GeneralSecurityException e) {
                    log.error("Error encrypting reset code for {}", normalizedEmail, e);
                }
            }
        } else {
            log.info("Forgot password requested for non-existent email {}", normalizedEmail);
        }

        return MessageResponse.builder()
                .message("If an account with this email exists, a password reset code has been sent.")
                .build();
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        User user = userDao.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("User with email " + normalizedEmail + " not found"));

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new IllegalArgumentException("Account is suspended");
        }

        List<UserActivationCode> codes = activationCodeDao.findByUserId(user.getId());
        OffsetDateTime now = OffsetDateTime.now();

        boolean hasExhaustedCode = codes.stream()
                .anyMatch(c -> c.getAttemptsCount() >= MAX_CODE_ATTEMPTS && c.getUsedAt() == null);

        List<UserActivationCode> activeCodes = codes.stream()
                .filter(c -> c.getUsedAt() == null && c.getExpiresAt().isAfter(now) && c.getAttemptsCount() < MAX_CODE_ATTEMPTS)
                .toList();

        if (activeCodes.isEmpty()) {
            if (hasExhaustedCode) {
                throw new IllegalArgumentException("Maximum verification attempts (10) exceeded. Please request a new code.");
            }
            throw new IllegalArgumentException("Invalid or expired password reset code");
        }

        UserActivationCode matchedCode = null;
        UserActivationCode activeCodeToIncrement = null;

        for (UserActivationCode code : activeCodes) {
            activeCodeToIncrement = code;

            try {
                CodeEncryptor codeEncryptor = codeEncryptorFactory.getEncryptor(code.getEncryptorVersion());
                String decryptedCode = codeEncryptor.decrypt(code.getCode(), activationSecret);
                if (decryptedCode.equals(request.getCode().trim())) {
                    matchedCode = code;
                    break;
                }
            } catch (GeneralSecurityException e) {
                log.error("Failed to decrypt code for user {}", normalizedEmail, e);
            }
        }

        if (matchedCode == null) {
            if (activeCodeToIncrement != null) {
                int newAttempts = activeCodeToIncrement.getAttemptsCount() + 1;
                activeCodeToIncrement.setAttempts(newAttempts);
                if (newAttempts >= MAX_CODE_ATTEMPTS) {
                    activeCodeToIncrement.setExpiresAt(now);
                    activationCodeDao.update(activeCodeToIncrement);
                    throw new IllegalArgumentException("Maximum verification attempts (10) exceeded. Please request a new code.");
                }
                activationCodeDao.update(activeCodeToIncrement);
            }
            throw new IllegalArgumentException("Invalid or expired password reset code");
        }

        matchedCode.setAttempts(matchedCode.getAttemptsCount() + 1);
        matchedCode.setUsedAt(now);
        activationCodeDao.update(matchedCode);

        PasswordHasher hasher = PasswordHasherFactory.getHasher();
        String salt = hasher.generateSalt();
        try {
            String hash = hasher.generateHashForPassword(request.getNewPassword(), salt, pepper);
            user.setPasswordHash(hash);
            user.setSalt(salt);
            user.setHashVersion(hasher.hashVersion());
            userDao.update(user);
            log.info("Password successfully reset for user {}", normalizedEmail);

            return MessageResponse.builder()
                    .message("Password has been reset successfully")
                    .build();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Error generating password hash", e);
        }
    }

    private String generateActivationCode() {
        int code = 100_000 + SECURE_RANDOM.nextInt(900_000);
        return String.valueOf(code);
    }
}
