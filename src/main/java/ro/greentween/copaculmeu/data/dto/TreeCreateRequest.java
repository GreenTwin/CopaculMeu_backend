package ro.greentween.copaculmeu.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ro.greentween.copaculmeu.data.enums.HealthLevel;
import ro.greentween.copaculmeu.data.enums.WateringLevel;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payload for registering a new tree")
public class TreeCreateRequest {

    @Schema(description = "GPS Latitude coordinate", example = "44.4323", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double latitude;

    @Schema(description = "GPS Longitude coordinate", example = "26.1063", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double longitude;

    @Schema(description = "Tree botanical or common species name", example = "Quercus robur", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Species is required")
    @Size(max = 255, message = "Species must not exceed 255 characters")
    private String species;

    @Schema(description = "Observed health level of the tree", example = "GOOD", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Health level is required")
    private HealthLevel healthLevel;

    @Schema(description = "Current watering / soil moisture level", example = "OPTIMAL")
    private WateringLevel wateringLevel;

    @Schema(description = "Timestamp when the tree was last watered", example = "2026-10-23T10:00:00Z")
    private LocalDateTime lastWateredAt;

    @Schema(description = "Estimated height of the tree in meters", example = "8.5")
    @PositiveOrZero(message = "Height must be non-negative")
    private Double height;

    @Schema(description = "Estimated age of the tree in years", example = "25")
    @PositiveOrZero(message = "Age must be non-negative")
    private Integer age;

    @Schema(description = "Trunk circumference in centimeters", example = "110.0")
    @PositiveOrZero(message = "Trunk circumference must be non-negative")
    private Double trunkCircumference;

    @Schema(description = "Human-readable landmark or address description", example = "Cismigiu Park, near the central lake")
    @Size(max = 255, message = "Location description must not exceed 255 characters")
    private String locationDescription;

    @Schema(description = "Additional forestry observations, notes, or maintenance tags", example = "Healthy specimen, recently inspected")
    private String notes;
}
