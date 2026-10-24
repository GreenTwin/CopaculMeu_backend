package ro.greentween.copaculmeu.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ro.greentween.copaculmeu.data.enums.UserRole;
import ro.greentween.copaculmeu.data.enums.UserStatus;
import ro.greentween.copaculmeu.data.model.User;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User profile response")
public class UserResponse {

    @Schema(description = "Unique ID of the user", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private UUID id;

    @Schema(description = "User's email address", example = "maria.popescu@example.ro")
    private String email;

    @Schema(description = "User's full name", example = "Maria Popescu")
    private String name;

    @Schema(description = "Assigned user role", example = "USER")
    private UserRole role;

    @Schema(description = "Account confirmation status", example = "UNCONFIRMED")
    private UserStatus status;

    @Schema(description = "Account creation timestamp", example = "2026-10-24T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Account update timestamp", example = "2026-10-24T10:00:00")
    private LocalDateTime updatedAt;

    public static UserResponse fromEntity(User user) {
        if (user == null) {
            return null;
        }
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
