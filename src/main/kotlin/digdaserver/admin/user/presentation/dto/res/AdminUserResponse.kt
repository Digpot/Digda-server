package digdaserver.admin.user.presentation.dto.res

import digdaserver.domain.user.domain.entity.User
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "어드민용 사용자 정보")
data class AdminUserResponse(

    @Schema(description = "사용자 ID(UUID)")
    val userId: String,

    @Schema(description = "이메일")
    val email: String?,

    @Schema(description = "이름")
    val name: String,

    @Schema(description = "상태 메시지")
    val statusMessage: String?,

    @Schema(description = "프로필 이미지 URL")
    val profileImage: String?,

    @Schema(description = "소셜 프로바이더")
    val socialProvider: String,

    @Schema(description = "권한(USER/ADMIN)")
    val role: String,

    @Schema(description = "가입일시")
    val createdAt: LocalDateTime,

    @Schema(description = "수정일시")
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(user: User): AdminUserResponse = AdminUserResponse(
            userId = user.id.toString(),
            email = user.email,
            name = user.name,
            statusMessage = user.statusMessage,
            profileImage = user.profileImage,
            socialProvider = user.socialProvider.name,
            role = user.role.name,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt
        )
    }
}
