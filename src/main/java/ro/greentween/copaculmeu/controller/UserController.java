package ro.greentween.copaculmeu.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ro.greentween.copaculmeu.data.dto.ForgotPasswordRequest;
import ro.greentween.copaculmeu.data.dto.MessageResponse;
import ro.greentween.copaculmeu.data.dto.ResetPasswordRequest;
import ro.greentween.copaculmeu.data.dto.UserActivationRequest;
import ro.greentween.copaculmeu.data.dto.UserRegisterRequest;
import ro.greentween.copaculmeu.data.dto.UserResponse;
import ro.greentween.copaculmeu.data.enums.UserRole;
import ro.greentween.copaculmeu.security.UserPrincipal;
import ro.greentween.copaculmeu.service.UserService;

import java.net.URI;
import java.util.UUID;

@Tag(name = "Users", description = "Endpoints for user registration and account management")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ro.greentween.copaculmeu.service.AuthService authService;

    @Operation(summary = "User login", description = "Authenticates user credentials and returns a JWT access token.")
    @PostMapping("/login")
    public ResponseEntity<ro.greentween.copaculmeu.data.dto.AuthResponse> login(
            @Valid @RequestBody ro.greentween.copaculmeu.data.dto.LoginRequest request,
            jakarta.servlet.http.HttpServletResponse response) {
        return ResponseEntity.ok(authService.login(request, response));
    }

    @Operation(summary = "User logout", description = "Invalidates the current JWT token.")
    @PostMapping("/logout")
    public ResponseEntity<ro.greentween.copaculmeu.data.dto.LogoutResponse> logout(
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response) {
        return ResponseEntity.ok(authService.logout(request, response));
    }

    @Operation(summary = "Activate user account", description = "Activates a user account using the 6-digit activation code.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account activated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired activation code, or user not found")
    })
    @PostMapping("/activate")
    public ResponseEntity<UserResponse> activate(@Valid @RequestBody UserActivationRequest request) {
        return ResponseEntity.ok(userService.activateUser(request));
    }

    @Operation(summary = "Forgot password request", description = "Generates a reset code and dispatches a password reset email using the configured template.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset code dispatched if account exists")
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(userService.forgotPassword(request));
    }

    @Operation(summary = "Reset password", description = "Resets user password using the code sent via email.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired reset code, or invalid password")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(userService.resetPassword(request));
    }

    @Operation(summary = "Register a new user", description = "Creates a new user account with encrypted password, generates an activation code, and saves the code using the configured CodeEncryptor version.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error or email already in use")
    })
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRegisterRequest request) {
        UserResponse response = userService.registerUser(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .replacePath("/api/users/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Get user by ID", description = "Retrieves public profile details for a registered user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "400", description = "Invalid user ID"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "User unique identifier (UUID)", required = true) @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserResponse response = userService.getUserById(id);
        boolean isOwner = principal != null && principal.getId().equals(id);
        boolean isAdmin = principal != null && principal.getRole() == UserRole.ADMIN;
        if (!isOwner && !isAdmin) {
            response.setEmail(null);
        }
        return ResponseEntity.ok(response);
    }
}
