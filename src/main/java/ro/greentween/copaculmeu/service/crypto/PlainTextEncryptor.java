package ro.greentween.copaculmeu.service.crypto;

import org.springframework.stereotype.Component;

import java.security.GeneralSecurityException;

@Component
public class PlainTextEncryptor implements CodeEncryptor {
    public static final String VERSION = "PLAIN";

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public String encrypt(String code, String keyAsString) throws GeneralSecurityException {
        return code;
    }

    @Override
    public String decrypt(String code, String keyAsString) throws GeneralSecurityException {
        return code;
    }
}
