package ro.greentween.copaculmeu.service.social;

public record SocialIdentity(String subject, String email, boolean emailVerified, String name, String nonce) {
}
