package digdaserver.domain.oauth2.presentation.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "소셜 로그인 요청")
data class LoginRequest(

    @Schema(description = "소셜 프로바이더", example = "kakao")
    @field:NotBlank(message = "provider는 필수입니다")
    val provider: String,

    @Schema(description = "소셜 프로바이더 액세스 토큰")
    @field:NotBlank(message = "accessToken은 필수입니다")
    val accessToken: String,

    @Schema(description = "Apple Sign In 시 필수 ID 토큰")
    val idToken: String? = null
)
