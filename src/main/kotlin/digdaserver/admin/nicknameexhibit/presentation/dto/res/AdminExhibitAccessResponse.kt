package digdaserver.admin.nicknameexhibit.presentation.dto.res

import digdaserver.domain.nickname_exhibit.domain.entity.NicknameExhibitAccess
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "관리자 전시관 접근 허용 사용자")
data class AdminExhibitAccessResponse(

    @Schema(description = "사용자 ID")
    val userId: UUID,

    @Schema(description = "사용자 이름")
    val name: String,

    @Schema(description = "사용자 이메일")
    val email: String?,

    @Schema(description = "프로필 이미지 URL")
    val profileImage: String?,

    @Schema(description = "허용된 시각")
    val grantedAt: LocalDateTime
) {
    companion object {
        fun from(entity: NicknameExhibitAccess): AdminExhibitAccessResponse {
            val user = entity.user
            return AdminExhibitAccessResponse(
                userId = user.id,
                name = user.name,
                email = user.email,
                profileImage = user.profileImage,
                grantedAt = entity.createdAt
            )
        }
    }
}
