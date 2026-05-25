package digdaserver.domain.diary.presentation.dto.res

import digdaserver.domain.diary.domain.entity.DiaryReactionType

/**
 * 일기 1개에 대한 이모지 리액션 집계.
 *
 * @property type 리액션 종류 (enum name)
 * @property count 해당 종류 총 카운트
 * @property reactedByMe 현재 사용자가 이 종류를 눌렀는지
 */
data class DiaryReactionSummary(
    val type: DiaryReactionType,
    val count: Int,
    val reactedByMe: Boolean
)
