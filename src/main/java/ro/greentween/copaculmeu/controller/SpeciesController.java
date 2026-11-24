package ro.greentween.copaculmeu.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.greentween.copaculmeu.data.dto.TreeSpeciesResponse;
import ro.greentween.copaculmeu.service.SpeciesService;

import java.util.List;

@Tag(name = "Species", description = "Endpoints for retrieving and managing predefined tree species")
@RestController
@RequestMapping("/api/species")
@RequiredArgsConstructor
public class SpeciesController {

        private final SpeciesService speciesService;

        @Operation(summary = "List all predefined tree species", description = "Retrieve the list of approved tree species (e.g. 'Magnolia x soulangeana (Magnolie)'). Publicly accessible.")
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
                        @Parameter(description = "Species unique ID", required = true, example = "1") @PathVariable Long id) {
                return ResponseEntity.ok(speciesService.getSpeciesById(id));
        }
}
