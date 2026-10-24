package ro.greentween.copaculmeu.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ro.greentween.copaculmeu.data.enums.HealthLevel;
import ro.greentween.copaculmeu.data.model.Tree;
import ro.greentween.copaculmeu.data.enums.WateringLevel;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Tree details representation")
public class TreeResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Unique ID of the tree", example = "1")
    private Long id;

    @Schema(description = "GPS Latitude coordinate", example = "44.4323")
    private Double latitude;

    @Schema(description = "GPS Longitude coordinate", example = "26.1063")
    private Double longitude;

    @Schema(description = "Original GPS Latitude coordinate before check-in averaging", example = "44.4323")
    private Double originalLatitude;

    @Schema(description = "Original GPS Longitude coordinate before check-in averaging", example = "26.1063")
    private Double originalLongitude;

    @Schema(description = "Tree species", example = "Quercus robur")
    private String species;

    @Schema(description = "Health status of the tree", example = "GOOD")
    private HealthLevel healthLevel;

    @Schema(description = "Current watering status", example = "OPTIMAL")
    private WateringLevel wateringLevel;

    @Schema(description = "Timestamp when the tree was last watered", example = "2026-10-23T10:00:00")
    private LocalDateTime lastWateredAt;

    @Schema(description = "Height in meters", example = "8.5")
    private Double height;

    @Schema(description = "Age in years", example = "25")
    private Integer age;

    @Schema(description = "Trunk circumference in centimeters", example = "110.0")
    private Double trunkCircumference;

    @Schema(description = "Location / landmark description", example = "Cismigiu Park, near the central lake")
    private String locationDescription;

    @Schema(description = "Notes and forestry observations", example = "Healthy specimen, recently inspected")
    private String notes;

    @Schema(description = "Tree proposal and approval status", example = "APPROVED")
    private ro.greentween.copaculmeu.data.enums.TreeStatus status;

    @Schema(description = "User ID who proposed the tree", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    private java.util.UUID proposedByUserId;

    @Schema(description = "Admin user ID who reviewed the proposal", example = "b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22")
    private java.util.UUID reviewedByUserId;

    @Schema(description = "Review timestamp", example = "2026-10-24T10:00:00")
    private LocalDateTime reviewedAt;

    @Schema(description = "Rejection explanation if proposal was rejected", example = "Duplicate tree already recorded")
    private String rejectionReason;

    @Schema(description = "Record creation timestamp", example = "2026-10-23T20:15:00")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2026-10-23T20:15:00")
    private LocalDateTime updatedAt;

    public static TreeResponse fromEntity(Tree tree) {
        if (tree == null) {
            return null;
        }
        return TreeResponse.builder()
                .id(tree.getId())
                .latitude(tree.getLatitude())
                .longitude(tree.getLongitude())
                .originalLatitude(tree.getOriginalLatitude())
                .originalLongitude(tree.getOriginalLongitude())
                .species(tree.getSpecies())
                .healthLevel(tree.getHealthLevel())
                .wateringLevel(tree.getWateringLevel())
                .lastWateredAt(tree.getLastWateredAt())
                .height(tree.getHeight())
                .age(tree.getAge())
                .trunkCircumference(tree.getTrunkCircumference())
                .locationDescription(tree.getLocationDescription())
                .notes(tree.getNotes())
                .status(tree.getStatus())
                .proposedByUserId(tree.getProposedBy() != null ? tree.getProposedBy().getId() : null)
                .reviewedByUserId(tree.getReviewedBy() != null ? tree.getReviewedBy().getId() : null)
                .reviewedAt(tree.getReviewedAt())
                .rejectionReason(tree.getRejectionReason())
                .createdAt(tree.getCreatedAt())
                .updatedAt(tree.getUpdatedAt())
                .build();
    }
}
