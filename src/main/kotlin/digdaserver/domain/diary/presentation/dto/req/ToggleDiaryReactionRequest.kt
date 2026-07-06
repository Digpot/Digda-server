package digdaserver.domain.diary.presentation.dto.req

import digdaserver.domain.diary.domain.entity.DiaryReactionType

data class ToggleDiaryReactionRequest(
    val type: DiaryReactionType
)
