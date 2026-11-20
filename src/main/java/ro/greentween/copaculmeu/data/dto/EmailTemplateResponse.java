package ro.greentween.copaculmeu.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ro.greentween.copaculmeu.data.enums.EmailTemplateType;
import ro.greentween.copaculmeu.data.model.EmailTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Email template details response")
public class EmailTemplateResponse {

    @Schema(description = "Template unique ID", example = "00000000-0000-0000-0000-000000000001")
    private UUID id;

    @Schema(description = "Template identifier name/type", example = "REGISTRATION_CONFIRMATION")
    private EmailTemplateType name;

    @Schema(description = "Email subject line", example = "Codul tau de activare Copacul Meu")
    private String subject;

    @Schema(description = "Email content body supporting short codes like {{name}} and {{code}}")
    private String body;

    @Schema(description = "Template description", example = "Email template for sending activation code upon registration")
    private String description;

    @Schema(description = "Supported placeholder short codes", example = "[\"{{name}}\", \"{{code}}\", \"{{email}}\"]")
    private List<String> availablePlaceholders;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;

    public static EmailTemplateResponse fromEntity(EmailTemplate entity) {
        if (entity == null) {
            return null;
        }
        return EmailTemplateResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .subject(entity.getSubject())
                .body(entity.getBody())
                .description(entity.getDescription())
                .availablePlaceholders(List.of("{{name}}", "{{code}}", "{{email}}"))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
