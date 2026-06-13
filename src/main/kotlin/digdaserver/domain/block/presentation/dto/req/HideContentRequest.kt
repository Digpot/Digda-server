package digdaserver.domain.block.presentation.dto.req

import digdaserver.domain.block.domain.entity.HideTargetType

/** "이 게시물 숨기기" 요청. 신고 없이 개별 콘텐츠를 본인에게서만 숨긴다. */
data class HideContentRequest(
    val targetType: HideTargetType,
    val targetId: Long
)
