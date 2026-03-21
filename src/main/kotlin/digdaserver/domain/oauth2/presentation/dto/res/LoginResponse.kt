package digdaserver.domain.oauth2.presentation.dto.res

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "로그인 응답")
data class LoginResponse(

    @Schema(description = "JWT 액세스 토큰 (만료: 1시간)")
    val accessToken: String,

    @Schema(description = "리프레시 토큰 (만료: 30일)")
    val refreshToken: String,

    @Schema(description = "사용자 정보")
    val user: UserResponse,

    @Schema(description = "신규 가입 여부 (true면 약관 동의 화면으로)")
    val isNewUser: Boolean
)
