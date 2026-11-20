package ro.greentween.copaculmeu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ro.greentween.copaculmeu.data.dao.UserActivationCodeDao;
import ro.greentween.copaculmeu.data.dao.UserDao;
import ro.greentween.copaculmeu.data.dto.LoginRequest;
import ro.greentween.copaculmeu.data.dto.UserRegisterRequest;
import ro.greentween.copaculmeu.data.dto.UserResponse;
import ro.greentween.copaculmeu.data.enums.UserStatus;
import ro.greentween.copaculmeu.data.model.User;
import ro.greentween.copaculmeu.data.model.UserActivationCode;
import ro.greentween.copaculmeu.service.UserService;
import ro.greentween.copaculmeu.service.crypto.CodeEncryptor;
import ro.greentween.copaculmeu.service.crypto.CodeEncryptorFactory;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserDao userDao;

    @Autowired
    private UserActivationCodeDao activationCodeDao;

    @Autowired
    private CodeEncryptorFactory codeEncryptorFactory;

    @Autowired
    private UserService userService;

    @Value("${app.security.activation-code-secret:CopaculMeuActivationCodeSecretKey2026!}")
    private String activationSecret;

    @BeforeEach
    void setUp() {
        activationCodeDao.deleteAll();
        userDao.deleteAll();
    }

    @Test
    void shouldRegisterNewUserAndPersistEncryptedActivationCode() throws Exception {
        UserRegisterRequest request = UserRegisterRequest.builder()
                .email("dan.ionescu@example.ro")
                .password("SecureP@ssw0rd2026")
                .name("Dan Ionescu")
                .build();

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email", is("dan.ionescu@example.ro")))
                .andExpect(jsonPath("$.name", is("Dan Ionescu")))
                .andExpect(jsonPath("$.status", is("UNCONFIRMED")))
                .andExpect(jsonPath("$.role", is("USER")))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.salt").doesNotExist());

        // Verify user in database
        Optional<User> optionalUser = userDao.findByEmail("dan.ionescu@example.ro");
        assertThat(optionalUser).isPresent();
        User user = optionalUser.get();
        assertThat(user.getHashVersion()).isEqualTo("V1");
        assertThat(user.getPasswordHash()).isNotBlank();
        assertThat(user.getSalt()).isNotBlank();

        // Verify activation code in database
        List<UserActivationCode> activationCodes = activationCodeDao.findByUserId(user.getId());
        assertThat(activationCodes).hasSize(1);
        UserActivationCode activationCode = activationCodes.get(0);
        assertThat(activationCode.getEncryptorVersion()).isEqualTo("AES_V1");
        assertThat(activationCode.getCode()).isNotBlank();
        assertThat(activationCode.getExpiresAt()).isNotNull();

        // Verify that we can decrypt the code using the version persisted on the record
        CodeEncryptor encryptor = codeEncryptorFactory.getEncryptor(activationCode.getEncryptorVersion());
        String decryptedCode = encryptor.decrypt(activationCode.getCode(), activationSecret);
        assertThat(decryptedCode).matches("^\\d{6}$");
    }

    @Test
    void shouldFailRegisteringWithDuplicateEmail() throws Exception {
        UserRegisterRequest request = UserRegisterRequest.builder()
                .email("existing@example.ro")
                .password("SecureP@ssw0rd2026")
                .build();

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second registration with same email should fail
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("User with email existing@example.ro already exists")));
    }

    @Test
    void shouldFailRegisteringWithInvalidEmailOrShortPassword() throws Exception {
        UserRegisterRequest request = UserRegisterRequest.builder()
                .email("not-an-email")
                .password("short")
                .build();

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    @DisplayName("Admin should successfully retrieve all users paged")
    @WithMockUser(roles = "ADMIN")
    void adminShouldRetrieveAllUsersPaged() throws Exception {
        userService.registerUser(UserRegisterRequest.builder().email("user1@example.ro").password("Password123!").name("User 1").build());
        userService.registerUser(UserRegisterRequest.builder().email("user2@example.ro").password("Password123!").name("User 2").build());
        userService.registerUser(UserRegisterRequest.builder().email("user3@example.ro").password("Password123!").name("User 3").build());

        // Page 0, size 2 -> should return 2 users, totalElements = 3
        mockMvc.perform(get("/api/users")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.size", is(2)))
                .andExpect(jsonPath("$.number", is(0)));

        // Page 1, size 2 -> should return remaining 1 user
        mockMvc.perform(get("/api/users")
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.number", is(1)));
    }

    @Test
    @DisplayName("Admin should filter users by status")
    @WithMockUser(roles = "ADMIN")
    void adminShouldFilterUsersByStatus() throws Exception {
        UserResponse unconfirmed = userService.registerUser(UserRegisterRequest.builder().email("unconf@example.ro").password("Password123!").name("Unconfirmed").build());
        UserResponse active = userService.registerUser(UserRegisterRequest.builder().email("active@example.ro").password("Password123!").name("Active").build());
        userService.setUserStatus(active.getId(), UserStatus.ACTIVE);

        // Filter by UNCONFIRMED
        mockMvc.perform(get("/api/users")
                        .param("status", "UNCONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].email", is("unconf@example.ro")))
                .andExpect(jsonPath("$.content[0].status", is("UNCONFIRMED")));

        // Filter by ACTIVE
        mockMvc.perform(get("/api/users")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].email", is("active@example.ro")))
                .andExpect(jsonPath("$.content[0].status", is("ACTIVE")));
    }

    @Test
    @DisplayName("Admin confirms user account, changing status to ACTIVE, enabling user to login")
    @WithMockUser(roles = "ADMIN")
    void adminShouldApproveUserAccountEnablingLogin() throws Exception {
        // 1. User registers -> account is UNCONFIRMED
        UserRegisterRequest registerRequest = UserRegisterRequest.builder()
                .email("to.approve@example.ro")
                .password("ApprovedPass123!")
                .name("To Approve")
                .build();

        MvcResult regResult = mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("UNCONFIRMED")))
                .andReturn();

        UserResponse registered = objectMapper.readValue(regResult.getResponse().getContentAsString(), UserResponse.class);
        UUID userId = registered.getId();

        // 2. Login fails before admin confirmation
        LoginRequest loginRequest = LoginRequest.builder()
                .email("to.approve@example.ro")
                .password("ApprovedPass123!")
                .build();

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Account is not activated")));

        // 3. Admin confirms/approves user account
        mockMvc.perform(post("/api/users/" + userId + "/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(userId.toString())))
                .andExpect(jsonPath("$.email", is("to.approve@example.ro")))
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        // 4. Now the user can log in successfully
        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.user.status", is("ACTIVE")));
    }

    @Test
    @DisplayName("Admin approval fails if user is already ACTIVE")
    @WithMockUser(roles = "ADMIN")
    void shouldFailApprovingAlreadyActiveUser() throws Exception {
        UserResponse user = userService.registerUser(UserRegisterRequest.builder().email("already.active@example.ro").password("Password123!").name("Active User").build());
        userService.setUserStatus(user.getId(), UserStatus.ACTIVE);

        mockMvc.perform(post("/api/users/" + user.getId() + "/approve"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Only unconfirmed user accounts can be approved")));
    }

    @Test
    @DisplayName("Admin approval fails if user is SUSPENDED")
    @WithMockUser(roles = "ADMIN")
    void shouldFailApprovingSuspendedUser() throws Exception {
        UserResponse user = userService.registerUser(UserRegisterRequest.builder().email("suspended@example.ro").password("Password123!").name("Suspended User").build());
        userService.setUserStatus(user.getId(), UserStatus.SUSPENDED);

        mockMvc.perform(post("/api/users/" + user.getId() + "/approve"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Cannot approve a suspended user account")));
    }

    @Test
    @DisplayName("Admin approval returns 404 for non-existent user ID")
    @WithMockUser(roles = "ADMIN")
    void shouldReturn404WhenApprovingNonExistentUser() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        mockMvc.perform(post("/api/users/" + nonExistentId + "/approve"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Not Found")));
    }

    @Test
    @DisplayName("Normal user should be forbidden from retrieving all users")
    @WithMockUser(roles = "USER")
    void normalUserForbiddenFromRetrievingAllUsers() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request to retrieve all users should return 401")
    void unauthenticatedRequestForbiddenFromRetrievingAllUsers() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Normal user should be forbidden from approving a user account")
    @WithMockUser(roles = "USER")
    void normalUserForbiddenFromApprovingUser() throws Exception {
        UserResponse user = userService.registerUser(UserRegisterRequest.builder().email("unconf.user@example.ro").password("Password123!").name("User").build());

        mockMvc.perform(post("/api/users/" + user.getId() + "/approve"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request to approve a user should return 401")
    void unauthenticatedRequestForbiddenFromApprovingUser() throws Exception {
        UserResponse user = userService.registerUser(UserRegisterRequest.builder().email("unconf.user2@example.ro").password("Password123!").name("User").build());

        mockMvc.perform(post("/api/users/" + user.getId() + "/approve"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Admin can also use /api/admin/users endpoints directly")
    @WithMockUser(roles = "ADMIN")
    void adminCanUseAdminPrefixedEndpoints() throws Exception {
        UserResponse unconfirmed = userService.registerUser(UserRegisterRequest.builder()
                .email("admin.prefixed@example.ro")
                .password("Password123!")
                .name("Admin Prefixed")
                .build());

        // List via /api/admin/users
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].email", is("admin.prefixed@example.ro")));

        // Approve via /api/admin/users/{id}/approve
        mockMvc.perform(post("/api/admin/users/" + unconfirmed.getId() + "/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }
}
