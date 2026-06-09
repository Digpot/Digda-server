package digdaserver.admin.title.presentation.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import java.util.UUID

/** 어드민이 특정 사용자에게 칭호를 부여한다. */
@Schema(description = "칭호 부여 요청")
data class GrantTitleRequest(
    @field:Schema(description = "대상 사용자 id")
    val userId: UUID,

    @field:NotBlank
    @field:Schema(description = "칭호 코드", example = "region_gyeongnam")
    val code: String
)
