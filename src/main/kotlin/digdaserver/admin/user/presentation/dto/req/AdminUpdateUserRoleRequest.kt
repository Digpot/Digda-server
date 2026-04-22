package digdaserver.admin.user.presentation.dto.req

import digdaserver.domain.user.domain.entity.Role
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "사용자 권한 변경 요청")
data class AdminUpdateUserRoleRequest(

    @field:NotNull
    @Schema(description = "변경할 권한", example = "ADMIN")
    val role: Role
)
