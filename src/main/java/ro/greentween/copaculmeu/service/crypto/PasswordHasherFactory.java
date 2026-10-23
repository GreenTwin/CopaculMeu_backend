package ro.greentween.copaculmeu.service.crypto;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PasswordHasherFactory {
    private static final Map<String, PasswordHasher> HASHERS = Map.of(PBKDF2PasswordHasher.VERSION, new PBKDF2PasswordHasher());

    public static PasswordHasher getHasher() {
        return new PBKDF2PasswordHasher();
    }

    public static PasswordHasher getHasher(String version) {
        PasswordHasher hasher = HASHERS.get(version);
        if (hasher == null) {
            return getHasher();
        }
        return hasher;
    }
}
