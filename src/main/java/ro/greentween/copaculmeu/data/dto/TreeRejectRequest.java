package ro.greentween.copaculmeu.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request body for rejecting a proposed tree")
public class TreeRejectRequest {

    @Schema(description = "Reason for rejecting the tree proposal", example = "Tree already exists at these GPS coordinates")
    private String reason;
}
