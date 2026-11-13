package ro.greentween.copaculmeu.service;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ro.greentween.copaculmeu.data.dao.UserDao;
import ro.greentween.copaculmeu.data.dto.AuthResponse;
import ro.greentween.copaculmeu.data.enums.UserRole;
import ro.greentween.copaculmeu.data.enums.UserStatus;
import ro.greentween.copaculmeu.data.model.User;
import ro.greentween.copaculmeu.exception.SocialAuthException;
import ro.greentween.copaculmeu.service.social.IdTokenVerifier;
import ro.greentween.copaculmeu.service.social.SocialIdentity;
import ro.greentween.copaculmeu.service.social.SocialNonceService;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocialAuthService {

    private final UserDao userDao;
    private final IdTokenVerifier idTokenVerifier;
    private final SocialNonceService nonceService;
    private final AuthService authService;

    @Transactional
    public AuthResponse loginWithGoogle(String idToken, String nonceCookie, HttpServletResponse response) {
        SocialIdentity identity = idTokenVerifier.verify(idToken, nonceCookie);
        nonceService.consume(nonceCookie);

        User user = linkGoogleAccount(identity);
        AuthResponse authResponse = authService.issueSession(user, response);
        nonceService.clear(response);
        return authResponse;
    }

    private User linkGoogleAccount(SocialIdentity identity) {
        if (!StringUtils.hasText(identity.subject())) {
            throw new SocialAuthException(HttpStatus.UNAUTHORIZED, "Social sign-in failed");
        }

        User bySubject = userDao.findByGoogleSub(identity.subject()).orElse(null);
        User byEmail = StringUtils.hasText(identity.email())
                ? userDao.findByEmail(identity.email()).orElse(null)
                : null;

        if (bySubject != null && byEmail != null && !bySubject.getId().equals(byEmail.getId())) {
            throw new SocialAuthException(HttpStatus.CONFLICT, "This sign-in cannot be linked to the existing account.");
        }

        User user = bySubject != null ? bySubject : byEmail;
        if (user == null) {
            user = User.builder()
                    .email(identity.email())
                    .name(identity.name())
                    .googleSub(identity.subject())
                    .role(UserRole.USER)
                    .status(UserStatus.ACTIVE)
                    .build();
            user = userDao.persist(user);
            log.info("Created user {} from Google sign-in", user.getId());
            return user;
        }

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new SocialAuthException(HttpStatus.FORBIDDEN, "Account is suspended");
        }

        if (!StringUtils.hasText(user.getGoogleSub())) {
            user.setGoogleSub(identity.subject());
        }
        if (user.getStatus() == UserStatus.UNCONFIRMED) {
            user.setStatus(UserStatus.ACTIVE);
        }
        if (!StringUtils.hasText(user.getName()) && StringUtils.hasText(identity.name())) {
            user.setName(identity.name());
        }
        return userDao.update(user);
    }
}
