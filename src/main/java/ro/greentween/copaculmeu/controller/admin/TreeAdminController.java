package ro.greentween.copaculmeu.controller.admin;

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
import ro.greentween.copaculmeu.data.dto.TreeCreateRequest;
import ro.greentween.copaculmeu.data.dto.TreeRejectRequest;
import ro.greentween.copaculmeu.data.dto.TreeResponse;
import ro.greentween.copaculmeu.security.UserPrincipal;
import ro.greentween.copaculmeu.service.TreeService;

import java.net.URI;
import java.util.List;

@Tag(name = "Admin - Trees", description = "Administrator endpoints for directly registering and approving/rejecting urban trees")
@RestController
@RequestMapping({"/api/admin/trees", "/api/trees"})
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class TreeAdminController {

    private final TreeService treeService;

    @Operation(summary = "Register a new tree (Admin only)",
            description = "Directly add a new tree (Administrator privileges required). The tree is automatically approved.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tree created successfully",
                    content = @Content(schema = @Schema(implementation = TreeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid coordinate bounds"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Administrator role required")
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

    @Operation(summary = "List pending tree proposals (Admin only)",
            description = "Retrieve all proposed trees that are awaiting administrative approval.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of pending tree proposals"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Administrator role required")
    })
    @GetMapping("/proposals")
    public ResponseEntity<List<TreeResponse>> getPendingProposals() {
        return ResponseEntity.ok(treeService.getPendingProposals());
    }

    @Operation(summary = "Approve tree proposal (Admin only)",
            description = "Approve a proposed tree. The tree transitions to APPROVED status and becomes visible on public map queries.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tree proposal approved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Administrator role required"),
            @ApiResponse(responseCode = "404", description = "Tree proposal not found")
    })
    @PostMapping("/{id}/approve")
    public ResponseEntity<TreeResponse> approveTree(
            @Parameter(description = "Tree unique ID", required = true) @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(treeService.approveTreeProposal(id, principal != null ? principal.getId() : null));
    }

    @Operation(summary = "Reject tree proposal (Admin only)",
            description = "Reject a proposed tree with an optional explanation reason.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tree proposal rejected"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Administrator role required"),
            @ApiResponse(responseCode = "404", description = "Tree proposal not found")
    })
    @PostMapping("/{id}/reject")
    public ResponseEntity<TreeResponse> rejectTree(
            @Parameter(description = "Tree unique ID", required = true) @PathVariable Long id,
            @RequestBody(required = false) TreeRejectRequest rejectRequest,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        String reason = rejectRequest != null ? rejectRequest.getReason() : null;
        return ResponseEntity.ok(treeService.rejectTreeProposal(id, principal != null ? principal.getId() : null, reason));
    }
}
