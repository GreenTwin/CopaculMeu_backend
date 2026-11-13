package ro.greentween.copaculmeu.service.social;

public interface IdTokenVerifier {

    SocialIdentity verify(String idToken, String expectedNonce);
}
