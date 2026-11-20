package ro.greentween.copaculmeu.service.social;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import ro.greentween.copaculmeu.exception.SocialAuthException;

import java.security.PublicKey;
import java.util.Base64;
import java.util.Set;

@Component
public class GoogleIdTokenVerifier implements IdTokenVerifier {

    static final String JWKS_URL = "https://www.googleapis.com/oauth2/v3/certs";
    static final String ISSUER_HTTPS = "https://accounts.google.com";
    static final String ISSUER_PLAIN = "accounts.google.com";

    private final JwksClient jwksClient;
    private final String clientId;

    public GoogleIdTokenVerifier(JwksClient jwksClient, @Value("${app.auth.google.client-id:}") String clientId) {
        this.jwksClient = jwksClient;
        this.clientId = clientId == null ? "" : clientId.trim();
    }

    @Override
    public SocialIdentity verify(String idToken, String expectedNonce) {
        if (!StringUtils.hasText(clientId)) {
            throw new SocialAuthException(HttpStatus.SERVICE_UNAVAILABLE, "Social sign-in failed");
        }
        if (!StringUtils.hasText(expectedNonce)) {
            throw new SocialAuthException(HttpStatus.UNAUTHORIZED, "Social sign-in failed");
        }

        try {
            String kid = readKeyId(idToken);
            PublicKey key = jwksClient.getKey(JWKS_URL, kid);
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .clockSkewSeconds(60)
                    .build()
                    .parseSignedClaims(idToken)
                    .getPayload();

            String issuer = claims.getIssuer();
            if (!ISSUER_HTTPS.equals(issuer) && !ISSUER_PLAIN.equals(issuer)) {
                throw failed();
            }
            Set<String> audience = claims.getAudience();
            if (audience == null || !audience.contains(clientId)) {
                throw failed();
            }

            String nonce = claims.get("nonce", String.class);
            if (!expectedNonce.equals(nonce)) {
                throw failed();
            }

            String email = claims.get("email", String.class);
            if (!StringUtils.hasText(email) || !isVerified(claims.get("email_verified"))) {
                throw failed();
            }

            String name = claims.get("name", String.class);
            return new SocialIdentity(
                    claims.getSubject(),
                    email.trim().toLowerCase(),
                    true,
                    StringUtils.hasText(name) ? name.trim() : null,
                    nonce
            );
        } catch (SocialAuthException ex) {
            throw ex;
        } catch (ExpiredJwtException ex) {
            throw failed();
        } catch (JwtException | IllegalArgumentException ex) {
            throw failed();
        }
    }

    private static boolean isVerified(Object value) {
        if (value instanceof Boolean verified) {
            return verified;
        }
        return value != null && "true".equalsIgnoreCase(value.toString());
    }

    private static String readKeyId(String idToken) {
        String[] parts = idToken.split("\\.");
        if (parts.length != 3) {
            throw failed();
        }
        try {
            byte[] headerBytes = Base64.getUrlDecoder().decode(pad(parts[0]));
            String header = new String(headerBytes);
            int kidIndex = header.indexOf("\"kid\"");
            if (kidIndex < 0) {
                throw failed();
            }
            int colon = header.indexOf(':', kidIndex);
            int firstQuote = header.indexOf('"', colon + 1);
            int secondQuote = header.indexOf('"', firstQuote + 1);
            if (firstQuote < 0 || secondQuote < 0) {
                throw failed();
            }
            String kid = header.substring(firstQuote + 1, secondQuote);
            if (!StringUtils.hasText(kid)) {
                throw failed();
            }
            return kid;
        } catch (SocialAuthException ex) {
            throw ex;
        } catch (IllegalArgumentException ex) {
            throw failed();
        }
    }

    private static String pad(String value) {
        int padding = (4 - (value.length() % 4)) % 4;
        return value + "=".repeat(padding);
    }

    private static SocialAuthException failed() {
        return new SocialAuthException(HttpStatus.UNAUTHORIZED, "Social sign-in failed");
    }
}
