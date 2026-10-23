package ro.greentween.copaculmeu.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.greentween.copaculmeu.data.dao.UserDao;
import ro.greentween.copaculmeu.data.dto.AuthResponse;
import ro.greentween.copaculmeu.data.dto.LoginRequest;
import ro.greentween.copaculmeu.data.dto.LogoutResponse;
import ro.greentween.copaculmeu.data.dto.UserResponse;
import ro.greentween.copaculmeu.data.enums.UserStatus;
import ro.greentween.copaculmeu.data.model.User;
import ro.greentween.copaculmeu.service.crypto.PasswordHasher;
import ro.greentween.copaculmeu.service.crypto.PasswordHasherFactory;
import tech.petrepopescu.common.utils.CookieUtil;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserDao userDao;
    private final JwtService jwtService;

    @Value("${app.security.pepper:CopaculMeuPepper2026!#$TreeApp}")
    private String pepper;

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        User user = userDao.findByEmail(normalizedEmail).orElse(null);

        if (user == null) {
            log.warn("Failed login attempt for non-existent email: {}", normalizedEmail);
            throw new BadCredentialsException("Invalid email or password");
        }

        if (user.getStatus() == UserStatus.SUSPENDED) {
            log.warn("Login attempt for suspended user: {}", normalizedEmail);
            throw new BadCredentialsException("Account is suspended");
        }

        PasswordHasher hasher = PasswordHasherFactory.getHasher(user.getHashVersion());
        try {
            String computedHash = hasher.generateHashForPassword(request.getPassword(), user.getSalt(), pepper);
            boolean match = MessageDigest.isEqual(
                    computedHash.getBytes(StandardCharsets.UTF_8),
                    user.getPasswordHash().getBytes(StandardCharsets.UTF_8)
            );

            if (!match) {
                log.warn("Invalid password attempt for email: {}", normalizedEmail);
                throw new BadCredentialsException("Invalid email or password");
            }
        } catch (GeneralSecurityException e) {
            log.error("Error computing password hash during login for user: {}", normalizedEmail, e);
            throw new IllegalStateException("Authentication service error", e);
        }

        String token = jwtService.generateToken(user);

        if (response != null) {
            Cookie cookie = CookieUtil.buildSecureCookie(JwtService.AUTH_COOKIE_NAME, token);
            cookie.setPath("/");
            cookie.setMaxAge((int) jwtService.getExpirationSeconds());
            response.addCookie(cookie);
        }

        log.info("User {} successfully logged in", normalizedEmail);

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationSeconds())
                .user(UserResponse.fromEntity(user))
                .build();
    }

    @Transactional
    public LogoutResponse logout(HttpServletRequest request, HttpServletResponse response) {
        String token = jwtService.extractToken(request);

        if (token != null) {
            jwtService.invalidateToken(token, "LOGOUT");
        }

        if (response != null) {
            Cookie cookie = CookieUtil.buildSecureCookie(JwtService.AUTH_COOKIE_NAME, "");
            cookie.setPath("/");
            cookie.setMaxAge(0);
            response.addCookie(cookie);
        }

        return LogoutResponse.builder()
                .message("Logged out successfully")
                .build();
    }
}
