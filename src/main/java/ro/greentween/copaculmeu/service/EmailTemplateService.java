package ro.greentween.copaculmeu.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.greentween.copaculmeu.data.dao.EmailTemplateDao;
import ro.greentween.copaculmeu.data.dto.EmailTemplateResponse;
import ro.greentween.copaculmeu.data.dto.EmailTemplateUpdateRequest;
import ro.greentween.copaculmeu.data.enums.EmailTemplateType;
import ro.greentween.copaculmeu.data.model.EmailTemplate;
import ro.greentween.copaculmeu.exception.ResourceNotFoundException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final EmailTemplateDao emailTemplateDao;

    @Transactional(readOnly = true)
    public List<EmailTemplateResponse> getAllTemplates() {
        return emailTemplateDao.getAll().stream()
                .map(EmailTemplateResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public EmailTemplate getTemplateByName(EmailTemplateType type) {
        return emailTemplateDao.findByName(type)
                .orElseThrow(() -> new ResourceNotFoundException("Email template " + type + " not found"));
    }

    @Transactional(readOnly = true)
    public EmailTemplateResponse getTemplateResponseByName(EmailTemplateType type) {
        return EmailTemplateResponse.fromEntity(getTemplateByName(type));
    }

    @Transactional(readOnly = true)
    public EmailTemplateResponse getTemplateResponseById(UUID id) {
        EmailTemplate template = emailTemplateDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Email template with id " + id + " not found"));
        return EmailTemplateResponse.fromEntity(template);
    }

    @Transactional
    public EmailTemplateResponse updateTemplate(UUID id, EmailTemplateUpdateRequest request) {
        EmailTemplate template = emailTemplateDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Email template with id " + id + " not found"));

        template.setSubject(request.getSubject().trim());
        template.setBody(request.getBody().trim());
        if (request.getDescription() != null) {
            template.setDescription(request.getDescription().trim());
        }

        EmailTemplate updated = emailTemplateDao.update(template);
        log.info("Updated email template {} (id: {})", updated.getName(), updated.getId());
        return EmailTemplateResponse.fromEntity(updated);
    }

    @Transactional
    public EmailTemplateResponse updateTemplateByName(EmailTemplateType type, EmailTemplateUpdateRequest request) {
        EmailTemplate template = getTemplateByName(type);
        template.setSubject(request.getSubject().trim());
        template.setBody(request.getBody().trim());
        if (request.getDescription() != null) {
            template.setDescription(request.getDescription().trim());
        }

        EmailTemplate updated = emailTemplateDao.update(template);
        log.info("Updated email template {} by type", updated.getName());
        return EmailTemplateResponse.fromEntity(updated);
    }

    /**
     * Replaces short codes in the template text with provided values.
     * Supports formats: {{key}}, {{ key }}, [key], ${key}.
     */
    public String resolvePlaceholders(String templateText, Map<String, String> placeholders) {
        if (templateText == null || placeholders == null || placeholders.isEmpty()) {
            return templateText;
        }

        String result = templateText;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue() != null ? entry.getValue() : "";

            // Replace {{key}}, {{ key }}, [key], ${key}
            result = result.replace("{{" + key + "}}", value);
            result = result.replace("{{ " + key + " }}", value);
            result = result.replace("[" + key + "]", value);
            result = result.replace("${" + key + "}", value);
        }
        return result;
    }
}
