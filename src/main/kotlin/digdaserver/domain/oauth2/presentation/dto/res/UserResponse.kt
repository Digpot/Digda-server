package digdaserver.domain.oauth2.presentation.dto.res

import digdaserver.domain.user.domain.entity.User
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사용자 정보")
data class UserResponse(

    @Schema(description = "사용자 ID")
    val id: String,

    @Schema(description = "닉네임")
    val name: String,

    @Schema(description = "이메일")
    val email: String?,

    @Schema(description = "프로필 이미지 URL")
    val profileImage: String?,

    @Schema(description = "소셜 프로바이더", example = "kakao")
    val provider: String,

    @Schema(description = "가입일")
    val createdAt: String
) {
    companion object {
        fun from(user: User): UserResponse {
            return UserResponse(
                id = user.id.toString(),
                name = user.displayedName(),
                email = user.email,
                profileImage = user.profileImage,
                provider = user.socialProvider.value,
                createdAt = user.createdAt.toString()
            )
        }
    }
}
