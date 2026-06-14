package digdaserver.admin.user.presentation.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "사용자 서비스 이용 제한 설정 요청")
data class AdminUpdateUserRestrictionRequest(

    @field:NotNull
    @Schema(description = "true=제한, false=해제", example = "true")
    val restricted: Boolean
)
