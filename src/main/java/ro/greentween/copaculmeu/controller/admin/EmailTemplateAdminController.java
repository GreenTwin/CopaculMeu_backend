package ro.greentween.copaculmeu.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.greentween.copaculmeu.data.dto.EmailTemplateResponse;
import ro.greentween.copaculmeu.data.dto.EmailTemplateUpdateRequest;
import ro.greentween.copaculmeu.data.enums.EmailTemplateType;
import ro.greentween.copaculmeu.service.EmailTemplateService;

import java.util.List;
import java.util.UUID;

@Tag(name = "Admin - Email Templates", description = "Endpoints for managing email notification templates (Admin only)")
@RestController
@RequestMapping({"/api/admin/email-templates", "/api/email-templates"})
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class EmailTemplateAdminController {

    private final EmailTemplateService emailTemplateService;

    @Operation(summary = "Get all email templates (Admin only)",
            description = "Retrieves all configured email templates from the database.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of email templates"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Administrator privileges required")
    })
    @GetMapping
    public ResponseEntity<List<EmailTemplateResponse>> getAllTemplates() {
        return ResponseEntity.ok(emailTemplateService.getAllTemplates());
    }

    @Operation(summary = "Get email template by ID or Name (Admin only)",
            description = "Retrieves an email template by its UUID ID or name (e.g. REGISTRATION_CONFIRMATION, FORGOT_PASSWORD).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email template details"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Administrator privileges required"),
            @ApiResponse(responseCode = "404", description = "Template not found")
    })
    @GetMapping("/{idOrName}")
    public ResponseEntity<EmailTemplateResponse> getTemplate(
            @Parameter(description = "Template UUID or template name enum", required = true)
            @PathVariable String idOrName
    ) {
        try {
            UUID id = UUID.fromString(idOrName);
            return ResponseEntity.ok(emailTemplateService.getTemplateResponseById(id));
        } catch (IllegalArgumentException e) {
            EmailTemplateType type = EmailTemplateType.fromString(idOrName);
            return ResponseEntity.ok(emailTemplateService.getTemplateResponseByName(type));
        }
    }

    @Operation(summary = "Update an email template (Admin only)",
            description = "Updates the subject, body, or description of an existing email template.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email template updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Administrator privileges required"),
            @ApiResponse(responseCode = "404", description = "Template not found")
    })
    @RequestMapping(value = "/{idOrName}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<EmailTemplateResponse> updateTemplate(
            @Parameter(description = "Template UUID or template name enum", required = true)
            @PathVariable String idOrName,
            @Valid @RequestBody EmailTemplateUpdateRequest request
    ) {
        try {
            UUID id = UUID.fromString(idOrName);
            return ResponseEntity.ok(emailTemplateService.updateTemplate(id, request));
        } catch (IllegalArgumentException e) {
            EmailTemplateType type = EmailTemplateType.fromString(idOrName);
            return ResponseEntity.ok(emailTemplateService.updateTemplateByName(type, request));
        }
    }
}
