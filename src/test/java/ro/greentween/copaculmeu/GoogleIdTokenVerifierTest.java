package ro.greentween.copaculmeu;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import ro.greentween.copaculmeu.exception.SocialAuthException;
import ro.greentween.copaculmeu.service.social.GoogleIdTokenVerifier;
import ro.greentween.copaculmeu.service.social.JwksClient;
import ro.greentween.copaculmeu.service.social.SocialIdentity;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleIdTokenVerifierTest {

    private static final String CLIENT_ID = "test-client.apps.googleusercontent.com";

    private KeyPair keyPair;
    private GoogleIdTokenVerifier verifier;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
        JwksClient jwksClient = (jwksUrl, kid) -> {
            if (!"kid-1".equals(kid)) {
                throw new SocialAuthException(HttpStatus.UNAUTHORIZED, "Social sign-in failed");
            }
            return keyPair.getPublic();
        };
        verifier = new GoogleIdTokenVerifier(jwksClient, CLIENT_ID);
    }

    @Test
    void acceptsAValidGoogleToken() {
        SocialIdentity identity = verifier.verify(token(Map.of()), "nonce-1");

        assertThat(identity.subject()).isEqualTo("google-sub");
        assertThat(identity.email()).isEqualTo("ada@example.com");
        assertThat(identity.emailVerified()).isTrue();
        assertThat(identity.name()).isEqualTo("Ada Lovelace");
    }

    @Test
    void rejectsWrongAudience() {
        assertUnauthorized(token(Map.of("aud", "other-client")));
    }

    @Test
    void rejectsWrongIssuer() {
        assertUnauthorized(token(Map.of("iss", "https://evil.example")));
    }

    @Test
    void rejectsExpiredToken() {
        assertUnauthorized(token(Map.of("exp", Date.from(Instant.now().minusSeconds(120)))));
    }

    @Test
    void rejectsBadSignature() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair other = generator.generateKeyPair();
        String signedBySomeoneElse = signed(other, Map.of());
        assertUnauthorized(signedBySomeoneElse);
    }

    @Test
    void rejectsNonceMismatch() {
        assertUnauthorized(token(Map.of("nonce", "other-nonce")));
    }

    @Test
    void rejectsUnverifiedEmail() {
        assertUnauthorized(token(Map.of("email_verified", false)));
    }

    @Test
    void rejectsMissingEmail() {
        assertUnauthorized(token(Map.of("email", "")));
    }

    @Test
    void rejectsUnknownKey() {
        assertUnauthorized(token(Map.of("kid", "missing")));
    }

    private void assertUnauthorized(String idToken) {
        assertThatThrownBy(() -> verifier.verify(idToken, "nonce-1"))
                .isInstanceOf(SocialAuthException.class)
                .extracting(ex -> ((SocialAuthException) ex).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private String token(Map<String, Object> overrides) {
        return signed(keyPair, overrides);
    }

    private String signed(KeyPair keys, Map<String, Object> overrides) {
        String kid = String.valueOf(overrides.getOrDefault("kid", "kid-1"));
        String issuer = String.valueOf(overrides.getOrDefault("iss", "https://accounts.google.com"));
        String audience = String.valueOf(overrides.getOrDefault("aud", CLIENT_ID));
        String nonce = String.valueOf(overrides.getOrDefault("nonce", "nonce-1"));
        Object email = overrides.getOrDefault("email", "Ada@Example.com");
        Object verified = overrides.getOrDefault("email_verified", true);
        Date expiration = overrides.get("exp") instanceof Date date
                ? date
                : Date.from(Instant.now().plusSeconds(300));

        return Jwts.builder()
                .header().keyId(kid).and()
                .subject("google-sub")
                .claim("email", email)
                .claim("email_verified", verified)
                .claim("name", "Ada Lovelace")
                .claim("nonce", nonce)
                .issuer(issuer)
                .audience().add(audience).and()
                .expiration(expiration)
                .signWith(keys.getPrivate())
                .compact();
    }
}
