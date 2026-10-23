package ro.greentween.copaculmeu.service.crypto;

import java.security.GeneralSecurityException;

public interface CodeEncryptor {
    String getVersion();
    String encrypt(String code, String keyAsString) throws GeneralSecurityException;
    String decrypt(String code, String keyAsString) throws GeneralSecurityException;
}
