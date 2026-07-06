package digdaserver.admin.auth.presentation.dto.res

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "관리자 로그인 응답")
data class AdminLoginResponse(

    @Schema(description = "관리자 사용자 ID(UUID)")
    val adminId: String,

    @Schema(description = "관리자 이메일")
    val email: String,

    @Schema(description = "관리자 이름")
    val name: String,

    @Schema(description = "JWT Access Token")
    val accessToken: String,

    @Schema(description = "JWT Refresh Token")
    val refreshToken: String
) {
    companion object {
        fun of(
            adminId: String,
            email: String,
            name: String,
            accessToken: String,
            refreshToken: String
        ): AdminLoginResponse = AdminLoginResponse(
            adminId = adminId,
            email = email,
            name = name,
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }
}
