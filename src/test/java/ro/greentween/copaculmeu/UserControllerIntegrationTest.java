package ro.greentween.copaculmeu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.greentween.copaculmeu.data.dao.UserActivationCodeDao;
import ro.greentween.copaculmeu.data.dao.UserDao;
import ro.greentween.copaculmeu.data.dto.UserRegisterRequest;
import ro.greentween.copaculmeu.data.model.User;
import ro.greentween.copaculmeu.data.model.UserActivationCode;
import ro.greentween.copaculmeu.service.crypto.CodeEncryptor;
import ro.greentween.copaculmeu.service.crypto.CodeEncryptorFactory;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
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
                .andExpect(jsonPath("$.plan", is("FREE")))
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
}
