package ro.greentween.copaculmeu;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ro.greentween.copaculmeu.data.dao.RevokedTokenDao;
import ro.greentween.copaculmeu.data.dao.UserActivationCodeDao;
import ro.greentween.copaculmeu.data.dao.UserDao;
import ro.greentween.copaculmeu.data.dto.AuthResponse;
import ro.greentween.copaculmeu.data.dto.LoginRequest;
import ro.greentween.copaculmeu.data.dto.UserRegisterRequest;
import ro.greentween.copaculmeu.service.JwtService;
import ro.greentween.copaculmeu.service.UserService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AuthControllerIntegrationTest {

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

    @BeforeEach
    void setUp() {
        revokedTokenDao.deleteAll();
        activationCodeDao.deleteAll();
        userDao.deleteAll();
    }

    @Test
    @DisplayName("Should successfully login with valid credentials and return JWT token and cookie")
    void shouldLoginSuccessfully() throws Exception {
        // Given: registered user
        userService.registerUser(UserRegisterRequest.builder()
                .email("ana.popescu@example.ro")
                .password("SecurePass123!")
                .name("Ana Popescu")
                .build());

        LoginRequest loginRequest = LoginRequest.builder()
                .email("ana.popescu@example.ro")
                .password("SecurePass123!")
                .build();

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType", equalTo("Bearer")))
                .andExpect(jsonPath("$.expiresIn", greaterThan(0)))
                .andExpect(jsonPath("$.user.email", equalTo("ana.popescu@example.ro")))
                .andExpect(jsonPath("$.user.name", equalTo("Ana Popescu")))
                .andExpect(cookie().exists(JwtService.AUTH_COOKIE_NAME))
                .andExpect(cookie().httpOnly(JwtService.AUTH_COOKIE_NAME, true))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        AuthResponse authResponse = objectMapper.readValue(responseBody, AuthResponse.class);

        assertThat(authResponse.getAccessToken()).isNotBlank();
        assertThat(jwtService.isTokenValid(authResponse.getAccessToken())).isTrue();
    }

    @Test
    @DisplayName("Should reject login with invalid password")
    void shouldRejectInvalidPassword() throws Exception {
        userService.registerUser(UserRegisterRequest.builder()
                .email("user.fail@example.ro")
                .password("CorrectPassword123!")
                .name("Test User")
                .build());

        LoginRequest wrongPasswordRequest = LoginRequest.builder()
                .email("user.fail@example.ro")
                .password("WrongPassword123!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPasswordRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", equalTo("Unauthorized")))
                .andExpect(jsonPath("$.message", equalTo("Invalid email or password")));
    }

    @Test
    @DisplayName("Should reject login for non-existent email")
    void shouldRejectNonExistentEmail() throws Exception {
        LoginRequest nonExistentRequest = LoginRequest.builder()
                .email("doesnotexist@example.ro")
                .password("AnyPassword123!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nonExistentRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", equalTo("Unauthorized")))
                .andExpect(jsonPath("$.message", equalTo("Invalid email or password")));
    }

    @Test
    @DisplayName("Should successfully authenticate request with JWT and invalidate token on logout")
    void shouldAuthenticateAndInvalidateTokenOnLogout() throws Exception {
        // 1. Register and login
        userService.registerUser(UserRegisterRequest.builder()
                .email("session.user@example.ro")
                .password("SessionPass123!")
                .name("Session User")
                .build());

        LoginRequest loginRequest = LoginRequest.builder()
                .email("session.user@example.ro")
                .password("SessionPass123!")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(loginResult.getResponse().getContentAsString(), AuthResponse.class);
        String token = authResponse.getAccessToken();

        // 2. Token should be valid and allow accessing /api/auth/me
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", equalTo("session.user@example.ro")))
                .andExpect(jsonPath("$.name", equalTo("Session User")));

        // 3. Logout using Bearer token
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", equalTo("Logged out successfully")))
                .andExpect(cookie().maxAge(JwtService.AUTH_COOKIE_NAME, 0));

        // 4. Token must now be invalidated in the database
        assertThat(jwtService.isTokenValid(token)).isFalse();

        // 5. Subsequent access to /api/auth/me with revoked token must fail with 401
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should support login and logout via /api/users/login and /api/users/logout aliases")
    void shouldSupportUserEndpointAliases() throws Exception {
        userService.registerUser(UserRegisterRequest.builder()
                .email("alias.user@example.ro")
                .password("AliasPass123!")
                .name("Alias User")
                .build());

        LoginRequest loginRequest = LoginRequest.builder()
                .email("alias.user@example.ro")
                .password("AliasPass123!")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(loginResult.getResponse().getContentAsString(), AuthResponse.class);
        String token = authResponse.getAccessToken();

        assertThat(jwtService.isTokenValid(token)).isTrue();

        // Logout via /api/users/logout
        mockMvc.perform(post("/api/users/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", equalTo("Logged out successfully")));

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }
}
