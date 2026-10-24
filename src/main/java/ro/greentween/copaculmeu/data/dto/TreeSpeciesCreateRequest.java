package ro.greentween.copaculmeu.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payload for registering a new tree species")
public class TreeSpeciesCreateRequest {

    @NotBlank(message = "Display name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    @Schema(description = "Full formatted name e.g. 'Scientific (Common)'", example = "Magnolia x soulangeana (Magnolie)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "Scientific name is required")
    @Size(max = 255, message = "Scientific name must not exceed 255 characters")
    @Schema(description = "Scientific botanical name", example = "Magnolia x soulangeana", requiredMode = Schema.RequiredMode.REQUIRED)
    private String scientificName;

    @NotBlank(message = "Common name is required")
    @Size(max = 255, message = "Common name must not exceed 255 characters")
    @Schema(description = "Common vernacular name", example = "Magnolie", requiredMode = Schema.RequiredMode.REQUIRED)
    private String commonName;

    @Size(max = 500, message = "Aliases must not exceed 500 characters")
    @Schema(description = "Comma-separated alternate names or aliases", example = "Magnolia soulangeana, Saucer magnolia")
    private String aliases;
}
