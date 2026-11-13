package ro.greentween.copaculmeu.service.social;

import java.security.PublicKey;

public interface JwksClient {

    PublicKey getKey(String jwksUrl, String kid);
}
