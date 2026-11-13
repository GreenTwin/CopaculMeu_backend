package ro.greentween.copaculmeu;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ro.greentween.copaculmeu.data.dao.RevokedTokenDao;
import ro.greentween.copaculmeu.data.dao.UserActivationCodeDao;
import ro.greentween.copaculmeu.data.dao.UserDao;
import ro.greentween.copaculmeu.data.dto.UserRegisterRequest;
import ro.greentween.copaculmeu.data.dto.UserResponse;
import ro.greentween.copaculmeu.data.enums.UserRole;
import ro.greentween.copaculmeu.data.enums.UserStatus;
import ro.greentween.copaculmeu.data.model.User;
import ro.greentween.copaculmeu.exception.SocialAuthException;
import ro.greentween.copaculmeu.service.JwtService;
import ro.greentween.copaculmeu.service.UserService;
import ro.greentween.copaculmeu.service.social.IdTokenVerifier;
import ro.greentween.copaculmeu.service.social.SocialIdentity;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Import(SocialAuthIntegrationTest.StubVerifierConfig.class)
class SocialAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private UserDao userDao;

    @Autowired
    private UserActivationCodeDao activationCodeDao;

    @Autowired
    private RevokedTokenDao revokedTokenDao;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        StubVerifierConfig.next = null;
        StubVerifierConfig.error = null;
        revokedTokenDao.deleteAll();
        activationCodeDao.deleteAll();
        userDao.deleteAll();
    }

    @Test
    void createsAnActiveUserFromGoogle() throws Exception {
        StubVerifierConfig.next = identity("google-1", "ada@example.com", "Ada");

        MvcResult result = signIn();

        assertThat(userDao.findByEmail("ada@example.com")).isPresent();
        User user = userDao.findByGoogleSub("google-1").orElseThrow();
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.getPasswordHash()).isNull();
        String token = objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asString();
        assertThat(jwtService.isTokenValid(token)).isTrue();

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", equalTo("ada@example.com")));
    }

    @Test
    void secondLoginReturnsTheSameUser() throws Exception {
        StubVerifierConfig.next = identity("google-1", "ada@example.com", "Ada");
        signIn();
        signIn();

        assertThat(userDao.findByEmail("ada@example.com")).isPresent();
        assertThat(userDao.getAll()).hasSize(1);
    }

    @Test
    void linksGoogleOntoAnExistingPasswordAccount() throws Exception {
        UserResponse registered = userService.registerUser(UserRegisterRequest.builder()
                .email("ada@example.com")
                .password("SecurePass123!")
                .name("Ada")
                .build());
        userService.setUserStatus(registered.getId(), UserStatus.ACTIVE);
        String hashBefore = userDao.findByEmail("ada@example.com").orElseThrow().getPasswordHash();

        StubVerifierConfig.next = identity("google-1", "ada@example.com", "Ada Lovelace");
        signIn();

        User linked = userDao.findByEmail("ada@example.com").orElseThrow();
        assertThat(linked.getId()).isEqualTo(registered.getId());
        assertThat(linked.getGoogleSub()).isEqualTo("google-1");
        assertThat(linked.getPasswordHash()).isEqualTo(hashBefore);
        assertThat(linked.getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    void activatesAnUnconfirmedAccountWhenGoogleVerifiesTheEmail() throws Exception {
        userService.registerUser(UserRegisterRequest.builder()
                .email("ada@example.com")
                .password("SecurePass123!")
                .name("Ada")
                .build());
        StubVerifierConfig.next = identity("google-1", "ada@example.com", "Ada");

        signIn();

        assertThat(userDao.findByEmail("ada@example.com").orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void rejectsASuspendedAccount() throws Exception {
        UserResponse registered = userService.registerUser(UserRegisterRequest.builder()
                .email("ada@example.com")
                .password("SecurePass123!")
                .name("Ada")
                .build());
        userService.setUserStatus(registered.getId(), UserStatus.SUSPENDED);
        StubVerifierConfig.next = identity("google-1", "ada@example.com", "Ada");

        String nonce = issueNonce();
        mockMvc.perform(post("/api/auth/google")
                        .cookie(new Cookie("social_nonce", nonce))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"good-token\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", equalTo("Account is suspended")));
    }

    @Test
    void rejectsWhenSubjectAndEmailBelongToDifferentUsers() throws Exception {
        User first = save(User.builder()
                .email("first@example.com")
                .googleSub("google-1")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
        User second = save(User.builder()
                .email("second@example.com")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .passwordHash("hash")
                .salt("salt")
                .hashVersion("V1")
                .build());
        StubVerifierConfig.next = identity("google-1", "second@example.com", "Second");

        String nonce = issueNonce();
        mockMvc.perform(post("/api/auth/google")
                        .cookie(new Cookie("social_nonce", nonce))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"good-token\"}"))
                .andExpect(status().isConflict());

        assertThat(userDao.find(first.getId()).getEmail()).isEqualTo("first@example.com");
        assertThat(userDao.find(second.getId()).getGoogleSub()).isNull();
    }

    @Test
    void rejectsAMissingNonceCookie() throws Exception {
        StubVerifierConfig.next = identity("google-1", "ada@example.com", "Ada");

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"good-token\"}"))
                .andExpect(status().isUnauthorized());
        assertThat(userDao.findByEmail("ada@example.com")).isEmpty();
    }

    @Test
    void rejectsAReusedNonce() throws Exception {
        StubVerifierConfig.next = identity("google-1", "ada@example.com", "Ada");
        String nonce = issueNonce();
        postGoogle(nonce).andExpect(status().isOk());
        postGoogle(nonce).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsABlankToken() throws Exception {
        String nonce = issueNonce();
        mockMvc.perform(post("/api/auth/google")
                        .cookie(new Cookie("social_nonce", nonce))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void doesNotCreateAUserWhenVerificationFails() throws Exception {
        StubVerifierConfig.error = new SocialAuthException(HttpStatus.UNAUTHORIZED, "Social sign-in failed");
        String nonce = issueNonce();

        postGoogle(nonce).andExpect(status().isUnauthorized());
        assertThat(userDao.getAll()).isEmpty();
    }

    @Test
    void passwordLoginForASocialOnlyAccountLooksLikeAWrongPassword() throws Exception {
        StubVerifierConfig.next = identity("google-1", "ada@example.com", "Ada");
        signIn();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ada@example.com\",\"password\":\"whatever123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", equalTo("Invalid email or password")));
    }

    @Test
    void logoutRevokesTheGoogleSession() throws Exception {
        StubVerifierConfig.next = identity("google-1", "ada@example.com", "Ada");
        MvcResult result = signIn();
        String token = objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asString();

        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private MvcResult signIn() throws Exception {
        String nonce = issueNonce();
        return postGoogle(nonce)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType", equalTo("Bearer")))
                .andReturn();
    }

    private org.springframework.test.web.servlet.ResultActions postGoogle(String nonce) throws Exception {
        return mockMvc.perform(post("/api/auth/google")
                .cookie(new Cookie("social_nonce", nonce))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idToken\":\"good-token\"}"));
    }

    private String issueNonce() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/social/nonce"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("nonce").asString();
    }

    private User save(User user) {
        return new TransactionTemplate(transactionManager).execute(status -> userDao.persist(user));
    }

    private static SocialIdentity identity(String subject, String email, String name) {
        return new SocialIdentity(subject, email, true, name, "nonce");
    }

    @TestConfiguration
    static class StubVerifierConfig {
        static SocialIdentity next;
        static SocialAuthException error;

        @Bean
        @Primary
        IdTokenVerifier stubIdTokenVerifier() {
            return (idToken, expectedNonce) -> {
                if (error != null) {
                    throw error;
                }
                return next;
            };
        }
    }
}
