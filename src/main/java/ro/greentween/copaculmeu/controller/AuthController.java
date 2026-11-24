package ro.greentween.copaculmeu.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ro.greentween.copaculmeu.data.dto.AuthResponse;
import ro.greentween.copaculmeu.data.dto.ForgotPasswordRequest;
import ro.greentween.copaculmeu.data.dto.LoginRequest;
import ro.greentween.copaculmeu.data.dto.LogoutResponse;
import ro.greentween.copaculmeu.data.dto.MessageResponse;
import ro.greentween.copaculmeu.data.dto.ResetPasswordRequest;
import ro.greentween.copaculmeu.data.dto.SocialLoginRequest;
import ro.greentween.copaculmeu.data.dto.UserActivationRequest;
import ro.greentween.copaculmeu.data.dto.UserResponse;
import ro.greentween.copaculmeu.security.UserPrincipal;
import ro.greentween.copaculmeu.service.AuthService;
import ro.greentween.copaculmeu.service.SocialAuthService;
import ro.greentween.copaculmeu.service.UserService;
import ro.greentween.copaculmeu.service.social.SocialNonceService;
import tech.petrepopescu.common.utils.CookieUtil;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user authentication, JWT session tracking, and logout")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final SocialAuthService socialAuthService;
    private final SocialNonceService socialNonceService;

    @PostMapping("/social/nonce")
    @Operation(summary = "Issue a social sign-in nonce", description = "Returns a one-time nonce and sets it as an httpOnly cookie. The browser must pass that nonce to the identity provider.")
    public ResponseEntity<java.util.Map<String, String>> issueSocialNonce(HttpServletResponse response) {
        String nonce = socialNonceService.issue(response);
        return ResponseEntity.ok(java.util.Map.of("nonce", nonce));
    }

    @PostMapping("/google")
    @Operation(summary = "Sign in with Google", description = "Verifies a Google ID token and returns the application JWT. Requires the social_nonce cookie from POST /api/auth/social/nonce.")
    public ResponseEntity<AuthResponse> loginWithGoogle(@Valid @RequestBody SocialLoginRequest request,
                                                        HttpServletRequest httpRequest,
                                                        HttpServletResponse response) {
        String nonce = null;
        var cookie = CookieUtil.getCookie(httpRequest, SocialNonceService.COOKIE_NAME);
        if (cookie != null) {
            nonce = cookie.getValue();
        }
        return ResponseEntity.ok(socialAuthService.loginWithGoogle(request.getIdToken(), nonce, response));
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates user credentials and returns a JWT access token along with an HTTP-only secure cookie.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials or account suspended")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request, response);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/activate")
    @Operation(summary = "Activate user account", description = "Validates the activation code sent to the user's email and marks their account as ACTIVE.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account activated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid or expired activation code, or user not found")
    })
    public ResponseEntity<UserResponse> activate(@Valid @RequestBody UserActivationRequest request) {
        UserResponse response = userService.activateUser(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password request", description = "Generates a reset code and dispatches a password reset email using the configured template.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset code dispatched if account exists",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(userService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Resets user password using the code sent via email.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid or expired reset code, or invalid password")
    })
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(userService.resetPassword(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "User logout", description = "Invalidates the current JWT token in the revocation database and clears authentication cookies.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logged out successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = LogoutResponse.class)))
    })
    public ResponseEntity<LogoutResponse> logout(HttpServletRequest request, HttpServletResponse response) {
        LogoutResponse logoutResponse = authService.logout(request, response);
        return ResponseEntity.ok(logoutResponse);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user", description = "Retrieves profile of the currently authenticated user based on JWT.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Current user profile",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized or token has been revoked")
    })
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        UserResponse response = userService.getUserById(principal.getId());
        return ResponseEntity.ok(response);
    }
}
