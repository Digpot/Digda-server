package digdaserver.domain.oauth2.presentation.dto.res

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "토큰 갱신 응답")
data class RefreshResponse(

    @Schema(description = "새 액세스 토큰")
    val accessToken: String,

    @Schema(description = "새 리프레시 토큰 (Rotation 적용)")
    val refreshToken: String
)
