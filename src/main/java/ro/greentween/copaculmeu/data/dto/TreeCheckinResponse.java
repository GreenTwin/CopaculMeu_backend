package ro.greentween.copaculmeu.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ro.greentween.copaculmeu.data.enums.HealthLevel;
import ro.greentween.copaculmeu.data.enums.WateringLevel;
import ro.greentween.copaculmeu.data.model.TreeCheckin;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Tree check-in confirmation and details")
public class TreeCheckinResponse {

    @Schema(description = "Check-in record unique ID", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
    private UUID id;

    @Schema(description = "Tree ID", example = "1")
    private Long treeId;

    @Schema(description = "User ID who performed check-in", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    private UUID userId;

    @Schema(description = "GPS Latitude recorded at check-in", example = "44.4323")
    private Double latitude;

    @Schema(description = "GPS Longitude recorded at check-in", example = "26.1063")
    private Double longitude;

    @Schema(description = "Deviation distance in meters from original tree GPS coordinates", example = "0.85")
    private Double deviationMeters;

    @Schema(description = "Whether the check-in is valid (deviation within 2.0m radius)", example = "true")
    private Boolean valid;

    @Schema(description = "Watering level recorded", example = "OPTIMAL")
    private WateringLevel wateringLevel;

    @Schema(description = "Whether tree was watered by user", example = "true")
    private Boolean watered;

    @Schema(description = "Proposed species if any", example = "Quercus petraea")
    private String proposedSpecies;

    @Schema(description = "Observed health level if any", example = "GOOD")
    private HealthLevel healthLevel;

    @Schema(description = "Observed height in meters", example = "9.0")
    private Double height;

    @Schema(description = "Estimated age in years", example = "26")
    private Integer age;

    @Schema(description = "Trunk circumference in cm", example = "115.0")
    private Double trunkCircumference;

    @Schema(description = "Location description", example = "Cismigiu Park")
    private String locationDescription;

    @Schema(description = "Check-in notes", example = "Watered generously")
    private String notes;

    @Schema(description = "Timestamp when check-in occurred", example = "2026-10-24T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Current tree state after this check-in")
    private TreeResponse tree;

    public static TreeCheckinResponse fromEntity(TreeCheckin checkin) {
        if (checkin == null) {
            return null;
        }
        return TreeCheckinResponse.builder()
                .id(checkin.getId())
                .treeId(checkin.getTree() != null ? checkin.getTree().getId() : null)
                .userId(checkin.getUser() != null ? checkin.getUser().getId() : null)
                .latitude(checkin.getLatitude())
                .longitude(checkin.getLongitude())
                .deviationMeters(checkin.getDeviationMeters())
                .valid(checkin.getValid())
                .wateringLevel(checkin.getWateringLevel())
                .watered(checkin.getWatered())
                .proposedSpecies(checkin.getProposedSpecies())
                .healthLevel(checkin.getHealthLevel())
                .height(checkin.getHeight())
                .age(checkin.getAge())
                .trunkCircumference(checkin.getTrunkCircumference())
                .locationDescription(checkin.getLocationDescription())
                .notes(checkin.getNotes())
                .createdAt(checkin.getCreatedAt())
                .tree(checkin.getTree() != null ? TreeResponse.fromEntity(checkin.getTree()) : null)
                .build();
    }
}
