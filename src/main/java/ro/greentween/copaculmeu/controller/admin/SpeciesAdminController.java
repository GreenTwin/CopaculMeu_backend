package ro.greentween.copaculmeu.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ro.greentween.copaculmeu.data.dto.TreeSpeciesCreateRequest;
import ro.greentween.copaculmeu.data.dto.TreeSpeciesResponse;
import ro.greentween.copaculmeu.service.SpeciesService;

import java.net.URI;

@Tag(name = "Admin - Species", description = "Administrator endpoints for managing tree species")
@RestController
@RequestMapping({"/api/admin/species", "/api/species"})
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class SpeciesAdminController {

    private final SpeciesService speciesService;

    @Operation(summary = "Add a new tree species (Admin only)",
            description = "Add a new tree species to the database (Administrator privileges required).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Species created successfully",
                    content = @Content(schema = @Schema(implementation = TreeSpeciesResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or duplicate name"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Administrator role required")
    })
    @PostMapping
    public ResponseEntity<TreeSpeciesResponse> addSpecies(@Valid @RequestBody TreeSpeciesCreateRequest request) {
        TreeSpeciesResponse response = speciesService.addSpecies(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }
}
