package ro.greentween.copaculmeu.service.crypto;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class CodeEncryptorFactory {
    private final Map<String, CodeEncryptor> encryptors;

    public CodeEncryptorFactory(Set<CodeEncryptor> codeEncryptors) {
        encryptors = new HashMap<>();
        for (CodeEncryptor encryptor : codeEncryptors) {
            encryptors.put(encryptor.getVersion(), encryptor);
        }
    }

    public CodeEncryptor getEncryptor() {
        return getEncryptor(AESEncryptorV1.VERSION);
    }

    public CodeEncryptor getEncryptor(String version) {
        CodeEncryptor encryptor = encryptors.get(version);
        if (encryptor == null) {
            throw new IllegalArgumentException("Unknown encryptor for version: " + version);
        }
        return encryptor;
    }
}
