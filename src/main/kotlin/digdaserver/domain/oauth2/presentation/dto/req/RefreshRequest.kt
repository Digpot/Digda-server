package digdaserver.domain.oauth2.presentation.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "토큰 갱신 요청")
data class RefreshRequest(

    @Schema(description = "기존 리프레시 토큰")
    @field:NotBlank(message = "refreshToken은 필수입니다")
    val refreshToken: String
)
