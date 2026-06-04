package digdaserver.domain.nickname_exhibit.presentation.dto.res

import digdaserver.domain.nickname_exhibit.domain.entity.NicknameExhibit
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "전시관 별명 카드")
data class NicknameExhibitResponse(

    @Schema(description = "카드 ID")
    val id: Long,

    @Schema(description = "별명")
    val nickname: String,

    @Schema(description = "카드 앞면 이미지 URL")
    val imageUrl: String?,

    @Schema(description = "별명이 생긴 배경·역사·설명 (카드 뒷면)")
    val history: String
) {
    companion object {
        fun from(entity: NicknameExhibit): NicknameExhibitResponse = NicknameExhibitResponse(
            id = entity.id,
            nickname = entity.nickname,
            imageUrl = entity.imageUrl,
            history = entity.history
        )
    }
}
