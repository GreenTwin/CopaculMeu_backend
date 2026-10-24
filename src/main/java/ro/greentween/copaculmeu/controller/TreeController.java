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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ro.greentween.copaculmeu.data.dto.CheckinStatusResponse;
import ro.greentween.copaculmeu.data.dto.TreeCheckinRequest;
import ro.greentween.copaculmeu.data.dto.TreeCheckinResponse;
import ro.greentween.copaculmeu.data.dto.TreeCreateRequest;
import ro.greentween.copaculmeu.data.dto.TreeRejectRequest;
import ro.greentween.copaculmeu.data.dto.TreeResponse;
import ro.greentween.copaculmeu.data.dto.TreeSpeciesResponse;
import ro.greentween.copaculmeu.security.UserPrincipal;
import ro.greentween.copaculmeu.service.SpeciesService;
import ro.greentween.copaculmeu.service.TreeCheckinService;
import ro.greentween.copaculmeu.service.TreeService;

import java.net.URI;
import java.util.List;

@Tag(name = "Trees", description = "Endpoints for registering, proposing, inspecting, and approving urban trees")
@RestController
@RequestMapping("/api/trees")
@RequiredArgsConstructor
public class TreeController {

    private final TreeService treeService;
    private final TreeCheckinService treeCheckinService;
    private final SpeciesService speciesService;

    @Operation(summary = "Register a new tree (Admin only)",
            description = "Directly add a new tree (Administrator privileges required). The tree is automatically approved.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tree created successfully",
                    content = @Content(schema = @Schema(implementation = TreeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid coordinate bounds"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Administrator role required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<TreeResponse> createTree(@Valid @RequestBody TreeCreateRequest request) {
        TreeResponse response = treeService.createTree(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Propose a new tree (Users)",
            description = "Submit a proposed tree for administrative review (Authenticated users). The tree will be assigned PENDING_APPROVAL status until an administrator approves it.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tree proposed successfully awaiting review",
                    content = @Content(schema = @Schema(implementation = TreeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User authentication required")
    })
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/propose")
    public ResponseEntity<TreeResponse> proposeTree(
            @Valid @RequestBody TreeCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        TreeResponse response = treeService.proposeTree(request, principal != null ? principal.getId() : null);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .replacePath("/api/trees/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "List pending tree proposals (Admin only)",
            description = "Retrieve all proposed trees that are awaiting administrative approval.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of pending tree proposals"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Administrator role required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/proposals")
    public ResponseEntity<List<TreeResponse>> getPendingProposals() {
        return ResponseEntity.ok(treeService.getPendingProposals());
    }

    @Operation(summary = "Approve tree proposal (Admin only)",
            description = "Approve a proposed tree. The tree transitions to APPROVED status and becomes visible on public map queries.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tree proposal approved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Administrator role required"),
            @ApiResponse(responseCode = "404", description = "Tree proposal not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/approve")
    public ResponseEntity<TreeResponse> approveTree(
            @Parameter(description = "Tree unique ID", required = true) @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(treeService.approveTreeProposal(id, principal != null ? principal.getId() : null));
    }

    @Operation(summary = "Reject tree proposal (Admin only)",
            description = "Reject a proposed tree with an optional explanation reason.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tree proposal rejected"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Administrator role required"),
            @ApiResponse(responseCode = "404", description = "Tree proposal not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/reject")
    public ResponseEntity<TreeResponse> rejectTree(
            @Parameter(description = "Tree unique ID", required = true) @PathVariable Long id,
            @RequestBody(required = false) TreeRejectRequest rejectRequest,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        String reason = rejectRequest != null ? rejectRequest.getReason() : null;
        return ResponseEntity.ok(treeService.rejectTreeProposal(id, principal != null ? principal.getId() : null, reason));
    }

    @Operation(summary = "Query trees", description = "Retrieve approved trees with optional area filters. If bounding box parameters (minLat, maxLat, minLng, maxLng) or radial parameters (lat, lng, radiusMeters) are provided, results are filtered accordingly. Otherwise, all approved trees are returned.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of approved trees matching criteria")
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

    @Operation(summary = "Find trees in bounding box", description = "Retrieve all approved trees within a geographic bounding box. Commonly used for map viewports.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of approved trees in the bounding box"),
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

    @Operation(summary = "Find trees nearby", description = "Retrieve all approved trees within a specified radius (in meters) of a GPS point, ordered from closest to farthest.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of approved trees in radial proximity"),
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

    @Operation(summary = "Check in on a tree",
            description = "Submit a user check-in with GPS coordinates, watering level, watering flag, and optional species and measurement observations. Valid if within 2m radius.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Check-in recorded successfully",
                    content = @Content(schema = @Schema(implementation = TreeCheckinResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User authentication required"),
            @ApiResponse(responseCode = "404", description = "Tree not found")
    })
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/check-ins")
    public ResponseEntity<TreeCheckinResponse> checkIn(
            @Parameter(description = "Tree unique ID", required = true) @PathVariable Long id,
            @Valid @RequestBody TreeCheckinRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        TreeCheckinResponse response = treeCheckinService.checkIn(id, request, principal != null ? principal.getId() : null);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{checkinId}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Get check-ins for a tree", description = "Retrieve list of all check-in observations for a tree.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of tree check-ins"),
            @ApiResponse(responseCode = "404", description = "Tree not found")
    })
    @GetMapping("/{id}/check-ins")
    public ResponseEntity<List<TreeCheckinResponse>> getCheckins(
            @Parameter(description = "Tree unique ID", required = true) @PathVariable Long id
    ) {
        return ResponseEntity.ok(treeCheckinService.getCheckinsForTree(id));
    }

    @Operation(summary = "Check check-in availability for current user",
            description = "Check if the authenticated user can check in on this tree under the 24-hour rate limit rule.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Check-in eligibility status"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Tree not found")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/check-ins/status")
    public ResponseEntity<CheckinStatusResponse> getCheckinStatus(
            @Parameter(description = "Tree unique ID", required = true) @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(treeCheckinService.getCheckinStatus(id, principal != null ? principal.getId() : null));
    }

    @Operation(summary = "Get predefined tree species",
            description = "Retrieve the list of approved tree species (e.g. 'Magnolia x soulangeana (Magnolie)'). Alias for /api/species.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of predefined tree species")
    })
    @GetMapping("/species")
    public ResponseEntity<List<TreeSpeciesResponse>> getSpecies() {
        return ResponseEntity.ok(speciesService.getAllSpecies());
    }
}
