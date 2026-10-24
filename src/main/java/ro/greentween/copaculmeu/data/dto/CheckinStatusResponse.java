package ro.greentween.copaculmeu.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User check-in availability status for a specific tree")
public class CheckinStatusResponse {

    @Schema(description = "Whether the user is currently eligible to check in on this tree", example = "true")
    private Boolean canCheckin;

    @Schema(description = "Tree unique ID", example = "1")
    private Long treeId;

    @Schema(description = "Timestamp of the user's last valid check-in on this tree", example = "2026-10-24T10:00:00")
    private LocalDateTime lastCheckinAt;

    @Schema(description = "Timestamp when the user can next check in on this tree", example = "2026-10-25T10:00:00")
    private LocalDateTime nextAvailableCheckinAt;

    @Schema(description = "Remaining cooldown in seconds until the next check-in is allowed", example = "0")
    private Long remainingCooldownSeconds;

    @Schema(description = "Human-readable status or cooldown message", example = "Check-in is available")
    private String message;
}
