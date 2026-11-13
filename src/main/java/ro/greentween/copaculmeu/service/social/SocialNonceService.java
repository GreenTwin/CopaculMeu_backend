package ro.greentween.copaculmeu.service.social;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ro.greentween.copaculmeu.exception.SocialAuthException;

import java.time.Duration;
import java.util.UUID;

@Service
public class SocialNonceService {

    public static final String COOKIE_NAME = "social_nonce";

    private final Cache<String, Boolean> nonces = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .maximumSize(10_000)
            .build();

    public String issue(HttpServletResponse response) {
        String nonce = UUID.randomUUID().toString();
        nonces.put(nonce, Boolean.TRUE);
        writeCookie(response, nonce, 600);
        return nonce;
    }

    public void consume(String nonce) {
        if (nonce == null || nonces.asMap().remove(nonce) == null) {
            throw new SocialAuthException(HttpStatus.UNAUTHORIZED, "Social sign-in failed");
        }
    }

    public void clear(HttpServletResponse response) {
        writeCookie(response, "", 0);
    }

    private static void writeCookie(HttpServletResponse response, String value, int maxAgeSeconds) {
        String header = COOKIE_NAME + "=" + value
                + "; Path=/"
                + "; Max-Age=" + maxAgeSeconds
                + "; HttpOnly; Secure; SameSite=None; Partitioned";
        response.addHeader(HttpHeaders.SET_COOKIE, header);
    }
}
