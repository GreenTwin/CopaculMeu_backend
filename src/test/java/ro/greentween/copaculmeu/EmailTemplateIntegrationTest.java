package ro.greentween.copaculmeu;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ro.greentween.copaculmeu.data.dao.EmailTemplateDao;
import ro.greentween.copaculmeu.data.dao.UserActivationCodeDao;
import ro.greentween.copaculmeu.data.dao.UserDao;
import ro.greentween.copaculmeu.data.dto.*;
import ro.greentween.copaculmeu.data.enums.EmailTemplateType;
import ro.greentween.copaculmeu.data.enums.UserStatus;
import ro.greentween.copaculmeu.data.model.EmailTemplate;
import ro.greentween.copaculmeu.data.model.User;
import ro.greentween.copaculmeu.data.model.UserActivationCode;
import ro.greentween.copaculmeu.service.EmailService;
import ro.greentween.copaculmeu.service.EmailTemplateService;
import ro.greentween.copaculmeu.service.UserService;
import ro.greentween.copaculmeu.service.crypto.CodeEncryptor;
import ro.greentween.copaculmeu.service.crypto.CodeEncryptorFactory;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class EmailTemplateIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailTemplateDao emailTemplateDao;

    @Autowired
    private EmailTemplateService emailTemplateService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserDao userDao;

    @Autowired
    private UserActivationCodeDao activationCodeDao;

    @Autowired
    private CodeEncryptorFactory codeEncryptorFactory;

    @Autowired
    private EmailService emailService;

    @MockitoBean
    private JavaMailSender javaMailSender;

    private String activationSecret = "CopaculMeuActivationCodeSecretKey2026!";

    @BeforeEach
    void setUp() {
        activationCodeDao.deleteAll();
        userDao.deleteAll();

        when(javaMailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
    }

    @Test
    @DisplayName("Admin should list all default seeded email templates")
    @WithMockUser(roles = "ADMIN")
    void adminShouldListAllEmailTemplates() throws Exception {
        mockMvc.perform(get("/api/admin/email-templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[*].name", hasItems("REGISTRATION_CONFIRMATION", "FORGOT_PASSWORD")));
    }

    @Test
    @DisplayName("Admin should retrieve a single email template by name and by ID")
    @WithMockUser(roles = "ADMIN")
    void adminShouldGetTemplateByNameAndId() throws Exception {
        EmailTemplate template = emailTemplateDao.findByName(EmailTemplateType.REGISTRATION_CONFIRMATION).orElseThrow();

        // By name
        mockMvc.perform(get("/api/admin/email-templates/REGISTRATION_CONFIRMATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(template.getId().toString())))
                .andExpect(jsonPath("$.name", is("REGISTRATION_CONFIRMATION")))
                .andExpect(jsonPath("$.availablePlaceholders", hasItems("{{name}}", "{{code}}", "{{email}}")));

        // By ID
        mockMvc.perform(get("/api/admin/email-templates/" + template.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(template.getId().toString())))
                .andExpect(jsonPath("$.name", is("REGISTRATION_CONFIRMATION")));
    }

    @Test
    @DisplayName("Admin should update an email template")
    @WithMockUser(roles = "ADMIN")
    void adminShouldUpdateEmailTemplate() throws Exception {
        EmailTemplateUpdateRequest request = EmailTemplateUpdateRequest.builder()
                .subject("Noul tau cod de activare")
                .body("Salut {{name}}, noul tau cod este: {{code}}.")
                .description("Updated registration template")
                .build();

        mockMvc.perform(put("/api/admin/email-templates/REGISTRATION_CONFIRMATION")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject", is("Noul tau cod de activare")))
                .andExpect(jsonPath("$.body", is("Salut {{name}}, noul tau cod este: {{code}}.")));

        // Verify in database
        EmailTemplate updated = emailTemplateDao.findByName(EmailTemplateType.REGISTRATION_CONFIRMATION).orElseThrow();
        assertThat(updated.getSubject()).isEqualTo("Noul tau cod de activare");
        assertThat(updated.getBody()).isEqualTo("Salut {{name}}, noul tau cod este: {{code}}.");
    }

    @Test
    @DisplayName("Non-admin and unauthenticated users cannot manage email templates")
    void nonAdminCannotManageEmailTemplates() throws Exception {
        // Unauthenticated
        mockMvc.perform(get("/api/admin/email-templates"))
                .andExpect(status().isUnauthorized());

        EmailTemplateUpdateRequest request = EmailTemplateUpdateRequest.builder()
                .subject("Test")
                .body("Test")
                .build();

        mockMvc.perform(put("/api/admin/email-templates/REGISTRATION_CONFIRMATION")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Normal user is forbidden from managing email templates")
    @WithMockUser(roles = "USER")
    void normalUserForbiddenFromManagingEmailTemplates() throws Exception {
        mockMvc.perform(get("/api/admin/email-templates"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Placeholder resolver replaces multiple shortcode formats")
    void placeholderResolverShouldReplaceShortCodes() {
        String template = "Hello {{name}}, code is {{code}} or [code] or ${code} for {{email}}!";
        Map<String, String> placeholders = Map.of(
                "name", "Maria",
                "code", "654321",
                "email", "maria@example.ro"
        );

        String result = emailTemplateService.resolvePlaceholders(template, placeholders);
        assertThat(result).isEqualTo("Hello Maria, code is 654321 or 654321 or 654321 for maria@example.ro!");
    }

    @Test
    @DisplayName("Forgot password and reset password flow works with email template")
    void forgotPasswordAndResetPasswordFlow() throws Exception {
        // 1. Create and activate a user
        UserResponse registered = userService.registerUser(UserRegisterRequest.builder()
                .email("forgot.user@example.ro")
                .password("OldP@ssword123")
                .name("Forgot User")
                .build());
        userService.setUserStatus(registered.getId(), UserStatus.ACTIVE);

        // 2. Request forgot password
        ForgotPasswordRequest forgotRequest = ForgotPasswordRequest.builder()
                .email("forgot.user@example.ro")
                .build();

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("password reset code has been sent")));

        // 3. Retrieve generated code from database
        List<UserActivationCode> codes = activationCodeDao.findByUserId(registered.getId());
        assertThat(codes).isNotEmpty();
        UserActivationCode latestCode = codes.get(codes.size() - 1);

        CodeEncryptor encryptor = codeEncryptorFactory.getEncryptor(latestCode.getEncryptorVersion());
        String rawCode = encryptor.decrypt(latestCode.getCode(), activationSecret);
        assertThat(rawCode).matches("^\\d{6}$");

        // 4. Complete reset password
        ResetPasswordRequest resetRequest = ResetPasswordRequest.builder()
                .email("forgot.user@example.ro")
                .code(rawCode)
                .newPassword("BrandNewP@ss2026")
                .build();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Password has been reset successfully")));

        // 5. User can now log in with the new password
        LoginRequest loginRequest = LoginRequest.builder()
                .email("forgot.user@example.ro")
                .password("BrandNewP@ss2026")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()));

        // Old password no longer works
        LoginRequest oldLoginRequest = LoginRequest.builder()
                .email("forgot.user@example.ro")
                .password("OldP@ssword123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oldLoginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("SMTP sender constructs and dispatches MIME message with replaced short codes")
    void smtpEmailServiceDispatchesEmailWithShortCodes() {
        org.springframework.test.util.ReflectionTestUtils.setField(emailService, "mailEnabled", true);

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("stefan@example.ro")
                .name("Stefan Pop")
                .build();

        emailService.sendRegistrationConfirmation(user, "987654");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender, atLeastOnce()).send(captor.capture());
        MimeMessage sentMessage = captor.getValue();
        assertThat(sentMessage).isNotNull();
    }
}
