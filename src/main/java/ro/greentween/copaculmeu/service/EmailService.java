package ro.greentween.copaculmeu.service;

import ro.greentween.copaculmeu.data.enums.EmailTemplateType;
import ro.greentween.copaculmeu.data.model.User;

import java.util.Map;

public interface EmailService {

    void sendRegistrationConfirmation(User user, String confirmationCode);

    void sendForgotPassword(User user, String resetCode);

    void sendEmail(String toEmail, EmailTemplateType templateType, Map<String, String> placeholders);
}
