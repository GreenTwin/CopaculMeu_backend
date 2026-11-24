package ro.greentween.copaculmeu.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ro.greentween.copaculmeu.data.enums.EmailTemplateType;
import ro.greentween.copaculmeu.data.model.EmailTemplate;
import ro.greentween.copaculmeu.data.model.User;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmtpEmailService implements EmailService {

    private final EmailTemplateService templateService;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    @Value("${app.mail.from:noreply@copaculmeu.ro}")
    private String fromAddress;

    @Override
    public void sendRegistrationConfirmation(User user, String confirmationCode) {
        String displayName = StringUtils.hasText(user.getName()) ? user.getName().trim() : user.getEmail();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("name", displayName);
        placeholders.put("code", confirmationCode);
        placeholders.put("email", user.getEmail());

        sendEmail(user.getEmail(), EmailTemplateType.REGISTRATION_CONFIRMATION, placeholders);
    }

    @Override
    public void sendForgotPassword(User user, String resetCode) {
        String displayName = StringUtils.hasText(user.getName()) ? user.getName().trim() : user.getEmail();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("name", displayName);
        placeholders.put("code", resetCode);
        placeholders.put("email", user.getEmail());

        sendEmail(user.getEmail(), EmailTemplateType.FORGOT_PASSWORD, placeholders);
    }

    @Override
    public void sendEmail(String toEmail, EmailTemplateType templateType, Map<String, String> placeholders) {
        EmailTemplate template = templateService.getTemplateByName(templateType);
        String subject = templateService.resolvePlaceholders(template.getSubject(), placeholders);
        String body = templateService.resolvePlaceholders(template.getBody(), placeholders);

        if (!mailEnabled) {
            log.info("Email delivery is disabled (app.mail.enabled=false). Skipping SMTP dispatch for {} to {}", templateType, toEmail);
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("JavaMailSender bean is not available. Skipping sending {} email to {}", templateType, toEmail);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body, false);

            mailSender.send(message);
            log.info("Sent {} email to {}", templateType, toEmail);
        } catch (Exception e) {
            log.error("Failed to send {} email via SMTP to {}", templateType, toEmail, e);
        }
    }
}
