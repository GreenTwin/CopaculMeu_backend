package ro.greentween.copaculmeu.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ro.greentween.copaculmeu.data.enums.HealthLevel;
import ro.greentween.copaculmeu.data.model.Tree;
import ro.greentween.copaculmeu.data.enums.WateringLevel;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Tree details representation")
public class TreeResponse {

    @Schema(description = "Unique ID of the tree", example = "1")
    private Long id;

    @Schema(description = "GPS Latitude coordinate", example = "44.4323")
    private Double latitude;

    @Schema(description = "GPS Longitude coordinate", example = "26.1063")
    private Double longitude;

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
                .species(tree.getSpecies())
                .healthLevel(tree.getHealthLevel())
                .wateringLevel(tree.getWateringLevel())
                .lastWateredAt(tree.getLastWateredAt())
                .height(tree.getHeight())
                .age(tree.getAge())
                .trunkCircumference(tree.getTrunkCircumference())
                .locationDescription(tree.getLocationDescription())
                .notes(tree.getNotes())
                .createdAt(tree.getCreatedAt())
                .updatedAt(tree.getUpdatedAt())
                .build();
    }
}
