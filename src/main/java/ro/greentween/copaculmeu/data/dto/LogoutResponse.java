package ro.greentween.copaculmeu.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Logout response")
public class LogoutResponse {

    @Schema(description = "Status message", example = "Logged out successfully")
    @Builder.Default
    private String message = "Logged out successfully";
}
