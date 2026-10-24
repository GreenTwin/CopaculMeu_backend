package ro.greentween.copaculmeu.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import java.util.List;

@Tag(name = "Species", description = "Endpoints for retrieving and managing predefined tree species")
@RestController
@RequestMapping("/api/species")
@RequiredArgsConstructor
public class SpeciesController {

    private final SpeciesService speciesService;

    @Operation(summary = "List all predefined tree species",
            description = "Retrieve the list of approved tree species (e.g. 'Magnolia x soulangeana (Magnolie)'). Publicly accessible.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of predefined tree species")
    })
    @GetMapping
    public ResponseEntity<List<TreeSpeciesResponse>> getAllSpecies() {
        return ResponseEntity.ok(speciesService.getAllSpecies());
    }

    @Operation(summary = "Get species by ID", description = "Retrieve a specific tree species by its unique ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tree species details"),
            @ApiResponse(responseCode = "404", description = "Species not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TreeSpeciesResponse> getSpeciesById(
            @Parameter(description = "Species unique ID", required = true, example = "1") @PathVariable Long id
    ) {
        return ResponseEntity.ok(speciesService.getSpeciesById(id));
    }

    @Operation(summary = "Add a new tree species (Admin only)",
            description = "Add a new tree species to the database (Administrator privileges required).")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Species created successfully",
                    content = @Content(schema = @Schema(implementation = TreeSpeciesResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or duplicate name"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Administrator role required")
    })
    @PreAuthorize("hasRole('ADMIN')")
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
