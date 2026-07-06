package digdaserver.admin.nicknameexhibit.presentation.dto.res

import digdaserver.domain.nickname_exhibit.domain.entity.NicknameExhibit
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "관리자 전시관 별명 카드")
data class AdminNicknameExhibitResponse(

    @Schema(description = "카드 ID")
    val id: Long,

    @Schema(description = "별명")
    val nickname: String,

    @Schema(description = "이미지 URL")
    val imageUrl: String?,

    @Schema(description = "별명 역사/설명")
    val history: String,

    @Schema(description = "정렬 순서")
    val sortOrder: Int,

    @Schema(description = "생성 시각")
    val createdAt: LocalDateTime,

    @Schema(description = "수정 시각")
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(entity: NicknameExhibit): AdminNicknameExhibitResponse = AdminNicknameExhibitResponse(
            id = entity.id,
            nickname = entity.nickname,
            imageUrl = entity.imageUrl,
            history = entity.history,
            sortOrder = entity.sortOrder,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
}
