package ro.greentween.copaculmeu.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ro.greentween.copaculmeu.data.dto.TreeCreateRequest;
import ro.greentween.copaculmeu.data.dto.TreeResponse;
import ro.greentween.copaculmeu.service.TreeService;

import java.net.URI;
import java.util.List;

@Tag(name = "Trees", description = "Endpoints for registering, inspecting, and searching urban trees")
@RestController
@RequestMapping("/api/trees")
@RequiredArgsConstructor
public class TreeController {

    private final TreeService treeService;

    @Operation(summary = "Register a new tree", description = "Add a newly mapped tree with GPS coordinates, species, health level, watering status, and dimensions.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tree created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid coordinate bounds")
    })
    @PostMapping
    public ResponseEntity<TreeResponse> createTree(@Valid @RequestBody TreeCreateRequest request) {
        TreeResponse response = treeService.createTree(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Query trees", description = "Retrieve trees with optional area filters. If bounding box parameters (minLat, maxLat, minLng, maxLng) or radial parameters (lat, lng, radiusMeters) are provided, results are filtered accordingly. Otherwise, all trees are returned.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of trees matching the criteria")
    })
    @GetMapping
    public ResponseEntity<List<TreeResponse>> getTrees(
            @Parameter(description = "Minimum latitude (south bound)") @RequestParam(required = false) Double minLat,
            @Parameter(description = "Maximum latitude (north bound)") @RequestParam(required = false) Double maxLat,
            @Parameter(description = "Minimum longitude (west bound)") @RequestParam(required = false) Double minLng,
            @Parameter(description = "Maximum longitude (east bound)") @RequestParam(required = false) Double maxLng,
            @Parameter(description = "Center latitude for radial search") @RequestParam(required = false) Double lat,
            @Parameter(description = "Center longitude for radial search") @RequestParam(required = false) Double lng,
            @Parameter(description = "Radius in meters for radial search (e.g. 500)") @RequestParam(required = false) Double radiusMeters
    ) {
        if (minLat != null && maxLat != null && minLng != null && maxLng != null) {
            return ResponseEntity.ok(treeService.getTreesInBoundingBox(minLat, maxLat, minLng, maxLng));
        }

        if (lat != null && lng != null && radiusMeters != null) {
            return ResponseEntity.ok(treeService.getTreesInRadius(lat, lng, radiusMeters));
        }

        return ResponseEntity.ok(treeService.getAllTrees());
    }

    @Operation(summary = "Find trees in bounding box", description = "Retrieve all trees within a geographic bounding box defined by south-west and north-east corners. Commonly used for map viewports.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of trees in the bounding box"),
            @ApiResponse(responseCode = "400", description = "Invalid coordinate bounds")
    })
    @GetMapping("/area")
    public ResponseEntity<List<TreeResponse>> getTreesInArea(
            @Parameter(description = "Minimum latitude (south bound)", required = true, example = "44.4300") @RequestParam Double minLat,
            @Parameter(description = "Maximum latitude (north bound)", required = true, example = "44.4400") @RequestParam Double maxLat,
            @Parameter(description = "Minimum longitude (west bound)", required = true, example = "26.0900") @RequestParam Double minLng,
            @Parameter(description = "Maximum longitude (east bound)", required = true, example = "26.1100") @RequestParam Double maxLng
    ) {
        return ResponseEntity.ok(treeService.getTreesInBoundingBox(minLat, maxLat, minLng, maxLng));
    }

    @Operation(summary = "Find trees nearby", description = "Retrieve all trees within a specified radius (in meters) of a GPS point, ordered from closest to farthest.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of trees in the radial proximity"),
            @ApiResponse(responseCode = "400", description = "Invalid coordinates or negative radius")
    })
    @GetMapping("/nearby")
    public ResponseEntity<List<TreeResponse>> getTreesNearby(
            @Parameter(description = "Center latitude", required = true, example = "44.4320") @RequestParam Double lat,
            @Parameter(description = "Center longitude", required = true, example = "26.1000") @RequestParam Double lng,
            @Parameter(description = "Radius in meters", example = "1000") @RequestParam(defaultValue = "1000") Double radiusMeters
    ) {
        return ResponseEntity.ok(treeService.getTreesInRadius(lat, lng, radiusMeters));
    }

    @Operation(summary = "Get tree by ID", description = "Retrieve detailed information for a specific tree by its unique ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tree details"),
            @ApiResponse(responseCode = "404", description = "Tree not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TreeResponse> getTreeById(
            @Parameter(description = "Unique identifier of the tree", required = true, example = "1") @PathVariable Long id
    ) {
        return ResponseEntity.ok(treeService.getTreeById(id));
    }
}
