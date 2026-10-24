package ro.greentween.copaculmeu.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User account activation request")
public class UserActivationRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Schema(description = "Registered user email address", example = "petre.popescu@example.ro")
    private String email;

    @NotBlank(message = "Activation code is required")
    @Schema(description = "6-digit activation code received upon registration", example = "123456")
    private String code;
}
