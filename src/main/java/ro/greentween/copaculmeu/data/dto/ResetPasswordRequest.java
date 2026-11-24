package ro.greentween.copaculmeu.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request body for completing password reset")
public class ResetPasswordRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "Account email address", example = "maria.popescu@example.ro", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "Reset code is required")
    @Schema(description = "6-digit password reset code received via email", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Schema(description = "New secure password", example = "NewSecureP@ss2026", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;
}
