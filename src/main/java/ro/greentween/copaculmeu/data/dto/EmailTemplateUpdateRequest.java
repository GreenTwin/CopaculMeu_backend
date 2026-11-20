package ro.greentween.copaculmeu.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request body for updating an email template")
public class EmailTemplateUpdateRequest {

    @NotBlank(message = "Subject must not be blank")
    @Schema(description = "Updated email subject line", example = "Activare cont Copacul Meu", requiredMode = Schema.RequiredMode.REQUIRED)
    private String subject;

    @NotBlank(message = "Body must not be blank")
    @Schema(description = "Updated email body text (supports {{name}}, {{code}}, {{email}})", requiredMode = Schema.RequiredMode.REQUIRED)
    private String body;

    @Schema(description = "Optional description of the template")
    private String description;
}
