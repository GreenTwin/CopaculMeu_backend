package ro.greentween.copaculmeu.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import ro.greentween.copaculmeu.data.model.TreeSpecies;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Tree species item representation")
public class TreeSpeciesResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Unique ID of the species", example = "1")
    private Long id;

    @Schema(description = "Full formatted display name including vernacular name", example = "Magnolia x soulangeana (Magnolie)")
    private String name;

    @Schema(description = "Botanical scientific name", example = "Magnolia x soulangeana")
    private String scientificName;

    @Schema(description = "Vernacular / Romanian common name", example = "Magnolie")
    private String commonName;

    @Schema(description = "Known synonyms, alternate spellings, or aliases", example = "Magnolia soulangeana, Saucer magnolia")
    private String aliases;

    public static TreeSpeciesResponse fromEntity(TreeSpecies species) {
        if (species == null) {
            return null;
        }
        return TreeSpeciesResponse.builder()
                .id(species.getId())
                .name(species.getName())
                .scientificName(species.getScientificName())
                .commonName(species.getCommonName())
                .aliases(species.getAliases())
                .build();
    }
}
