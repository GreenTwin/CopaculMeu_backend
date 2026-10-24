package ro.greentween.copaculmeu.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ro.greentween.copaculmeu.data.enums.HealthLevel;
import ro.greentween.copaculmeu.data.enums.WateringLevel;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Tree check-in request with GPS coordinates and observation data")
public class TreeCheckinRequest {

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be greater than or equal to -90.0")
    @DecimalMax(value = "90.0", message = "Latitude must be less than or equal to 90.0")
    @Schema(description = "User's current GPS Latitude at the tree", example = "44.4323", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be greater than or equal to -180.0")
    @DecimalMax(value = "180.0", message = "Longitude must be less than or equal to 180.0")
    @Schema(description = "User's current GPS Longitude at the tree", example = "26.1063", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double longitude;

    @NotNull(message = "Watering level is required")
    @Schema(description = "Observed watering level of the tree", example = "OPTIMAL", requiredMode = Schema.RequiredMode.REQUIRED)
    private WateringLevel wateringLevel;

    @NotNull(message = "Watered flag is required (indicates if the user watered the tree)")
    @Schema(description = "Whether the user watered the tree during this check-in", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean watered;

    @Schema(description = "Optionally proposed new species for this tree", example = "Quercus petraea")
    private String proposedSpecies;

    @Schema(description = "Optionally updated tree health status", example = "EXCELLENT")
    private HealthLevel healthLevel;

    @Positive(message = "Height must be positive")
    @Schema(description = "Observed tree height in meters", example = "9.2")
    private Double height;

    @Positive(message = "Age must be positive")
    @Schema(description = "Estimated tree age in years", example = "26")
    private Integer age;

    @Positive(message = "Trunk circumference must be positive")
    @Schema(description = "Measured trunk circumference in centimeters", example = "115.0")
    private Double trunkCircumference;

    @Schema(description = "Updated location or landmark description", example = "Near the playground fountain")
    private String locationDescription;

    @Schema(description = "Observation notes or forestry comments", example = "Tree shows new spring buds, soil was dry before watering")
    private String notes;
}
