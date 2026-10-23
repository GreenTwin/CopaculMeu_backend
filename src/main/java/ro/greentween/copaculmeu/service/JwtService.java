package ro.greentween.copaculmeu.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ro.greentween.copaculmeu.data.dao.RevokedTokenDao;
import ro.greentween.copaculmeu.data.dao.UserDao;
import ro.greentween.copaculmeu.data.model.RevokedToken;
import ro.greentween.copaculmeu.data.model.User;
import tech.petrepopescu.common.utils.CookieUtil;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    public static final String AUTH_COOKIE_NAME = "jwt";
    public static final String BEARER_PREFIX = "Bearer ";

    private final RevokedTokenDao revokedTokenDao;
    private final UserDao userDao;

    @Value("${app.security.jwt.secret:CopaculMeuUltraSecureJwtSecretKeyMustBeAtLeast256BitsLongForHmacSha256Security2026!}")
    private String jwtSecret;

    @Value("${app.security.jwt.expiration-seconds:86400}")
    private long expirationSeconds;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    /**
     * Generates a signed JWT for the provided user
     */
    public String generateToken(User user) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + (expirationSeconds * 1000L));
        String tokenId = UUID.randomUUID().toString();

        return Jwts.builder()
                .id(tokenId)
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("status", user.getStatus().name())
                .claim("name", user.getName())
                .issuedAt(now)
                .expiration(validity)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Parses and verifies claims from token
     */
    public Optional<Claims> parseTokenClaims(String token) {
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT parsing failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Validates whether a token is valid, correctly signed, not expired, and not revoked
     */
    public boolean isTokenValid(String token) {
        Optional<Claims> claimsOpt = parseTokenClaims(token);
        if (claimsOpt.isEmpty()) {
            return false;
        }

        Claims claims = claimsOpt.get();
        String tokenId = claims.getId();
        if (!StringUtils.hasText(tokenId)) {
            return false;
        }

        return !revokedTokenDao.isTokenRevoked(tokenId);
    }

    /**
     * Invalidates a token by adding its unique token ID (jti) to the revoked_tokens store
     */
    @Transactional
    public boolean invalidateToken(String token, String reason) {
        if (!StringUtils.hasText(token)) {
            return false;
        }

        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            claims = e.getClaims();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Cannot invalidate unparseable token: {}", e.getMessage());
            return false;
        }

        String tokenId = claims.getId();
        if (!StringUtils.hasText(tokenId)) {
            return false;
        }

        if (revokedTokenDao.isTokenRevoked(tokenId)) {
            return true; // Already revoked
        }

        UUID userId = UUID.fromString(claims.getSubject());
        User user = userDao.find(userId);
        if (user == null) {
            log.warn("User {} for token {} does not exist", userId, tokenId);
            return false;
        }

        Date expiration = claims.getExpiration() != null ? claims.getExpiration() : new Date();
        OffsetDateTime expiresAt = OffsetDateTime.ofInstant(expiration.toInstant(), ZoneOffset.UTC);

        RevokedToken revokedToken = RevokedToken.builder()
                .tokenId(tokenId)
                .user(user)
                .expiresAt(expiresAt)
                .reason(reason != null ? reason : "LOGOUT")
                .build();

        revokedTokenDao.persist(revokedToken);
        log.info("Token {} for user {} successfully invalidated", tokenId, user.getEmail());
        return true;
    }

    /**
     * Extracts token from either the Authorization header or the secure cookie
     */
    public String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length()).trim();
        }

        Cookie cookie = CookieUtil.getCookie(request, AUTH_COOKIE_NAME);
        if (cookie != null && StringUtils.hasText(cookie.getValue())) {
            return cookie.getValue().trim();
        }

        return null;
    }
}
