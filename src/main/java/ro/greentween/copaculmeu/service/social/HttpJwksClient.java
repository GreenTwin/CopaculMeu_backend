package ro.greentween.copaculmeu.service.social;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import ro.greentween.copaculmeu.exception.SocialAuthException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class HttpJwksClient implements JwksClient {

    private static final Duration CACHE_TTL = Duration.ofHours(1);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ConcurrentHashMap<String, CachedKeys> cache = new ConcurrentHashMap<>();

    public HttpJwksClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public PublicKey getKey(String jwksUrl, String kid) {
        CachedKeys cached = cache.get(jwksUrl);
        if (cached != null && cached.isFresh() && cached.keys.containsKey(kid)) {
            return cached.keys.get(kid);
        }

        CachedKeys loaded = load(jwksUrl);
        cache.put(jwksUrl, loaded);
        PublicKey key = loaded.keys.get(kid);
        if (key == null) {
            throw new SocialAuthException(HttpStatus.UNAUTHORIZED, "Social sign-in failed");
        }
        return key;
    }

    private CachedKeys load(String jwksUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(jwksUrl))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new SocialAuthException(HttpStatus.SERVICE_UNAVAILABLE, "Social sign-in failed");
            }
            JsonNode keys = objectMapper.readTree(response.body()).path("keys");
            Map<String, PublicKey> parsed = new HashMap<>();
            if (keys.isArray()) {
                for (JsonNode key : keys) {
                    if (!"RSA".equals(key.path("kty").asString())) {
                        continue;
                    }
                    String kid = key.path("kid").asString(null);
                    String modulus = key.path("n").asString(null);
                    String exponent = key.path("e").asString(null);
                    if (kid == null || modulus == null || exponent == null) {
                        continue;
                    }
                    parsed.put(kid, rsaPublicKey(modulus, exponent));
                }
            }
            return new CachedKeys(parsed, Instant.now().plus(CACHE_TTL));
        } catch (SocialAuthException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SocialAuthException(HttpStatus.SERVICE_UNAVAILABLE, "Social sign-in failed");
        }
    }

    static PublicKey rsaPublicKey(String modulus, String exponent) throws Exception {
        BigInteger n = new BigInteger(1, decode(modulus));
        BigInteger e = new BigInteger(1, decode(exponent));
        return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(n, e));
    }

    private static byte[] decode(String value) {
        int padding = (4 - (value.length() % 4)) % 4;
        return Base64.getUrlDecoder().decode(value + "=".repeat(padding));
    }

    private record CachedKeys(Map<String, PublicKey> keys, Instant expiresAt) {
        boolean isFresh() {
            return Instant.now().isBefore(expiresAt);
        }
    }
}
