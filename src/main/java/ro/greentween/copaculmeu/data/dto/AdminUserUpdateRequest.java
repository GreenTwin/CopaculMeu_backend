package ro.greentween.copaculmeu.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Admin edit of a user's name and role")
public class AdminUserUpdateRequest {

    @Schema(description = "New display name. Omitted leaves the current name. Blank clears it.", example = "Maria Popescu")
    private String name;

    @Schema(description = "USER or ADMIN. Omitted leaves the current role.", example = "USER")
    private String role;
}
