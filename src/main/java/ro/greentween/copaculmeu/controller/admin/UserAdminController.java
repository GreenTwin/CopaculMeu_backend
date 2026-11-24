package ro.greentween.copaculmeu.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.greentween.copaculmeu.data.dto.UserResponse;
import ro.greentween.copaculmeu.data.enums.UserRole;
import ro.greentween.copaculmeu.data.enums.UserStatus;
import ro.greentween.copaculmeu.service.UserService;

import java.util.UUID;

@Tag(name = "Admin - Users", description = "Administrator endpoints for managing user accounts and confirmations")
@RestController
@RequestMapping({"/api/admin/users", "/api/users"})
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminController {

    private final UserService userService;

    @Operation(summary = "Get all users paged (Admin only)",
            description = "Retrieves a paged list of registered users with optional status and role filtering. Administrator privileges required.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paged list of users"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Administrator privileges required")
    })
    @GetMapping({"", "/", "/paged"})
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @Parameter(description = "Zero-based page index (0..N)") @RequestParam(required = false) Integer page,
            @Parameter(description = "The size of the page to be returned") @RequestParam(required = false) Integer size,
            @Parameter(description = "Maximum number of results to return (alternative to size)") @RequestParam(required = false) Integer limit,
            @Parameter(description = "Offset for results (alternative to page)") @RequestParam(required = false) Integer offset,
            @Parameter(description = "Filter by user status (e.g. UNCONFIRMED, ACTIVE, SUSPENDED)") @RequestParam(required = false) UserStatus status,
            @Parameter(description = "Filter by user role (e.g. USER, ADMIN)") @RequestParam(required = false) UserRole role
    ) {
        return ResponseEntity.ok(userService.getAllUsers(page, size, limit, offset, status, role));
    }

    @Operation(summary = "Approve user account (Admin only)",
            description = "Approves a registered user account, changing its status from UNCONFIRMED to ACTIVE. Administrator privileges required.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User account approved successfully"),
            @ApiResponse(responseCode = "400", description = "User account is not unconfirmed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Administrator privileges required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @RequestMapping(value = {"/{id}/approve", "/{id}/confirm"}, method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<UserResponse> approveUser(
            @Parameter(description = "User unique identifier (UUID)", required = true) @PathVariable UUID id
    ) {
        return ResponseEntity.ok(userService.approveUser(id));
    }

    @Operation(summary = "Change user status (Admin only)",
            description = "Updates the confirmation status of a user account (UNCONFIRMED, ACTIVE, SUSPENDED). Administrator privileges required.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status or user ID"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Administrator privileges required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @RequestMapping(value = "/{id}/status", method = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<UserResponse> setUserStatus(
            @Parameter(description = "User unique identifier (UUID)", required = true) @PathVariable UUID id,
            @Parameter(description = "Target user status (UNCONFIRMED, ACTIVE, SUSPENDED)") @RequestParam(required = false) UserStatus status,
            @RequestBody(required = false) java.util.Map<String, String> body
    ) {
        UserStatus targetStatus = status;
        if (targetStatus == null && body != null && body.containsKey("status")) {
            targetStatus = UserStatus.valueOf(body.get("status").toUpperCase());
        }
        if (targetStatus == null) {
            throw new IllegalArgumentException("User status must be provided");
        }
        return ResponseEntity.ok(userService.setUserStatus(id, targetStatus));
    }
}
