package digdaserver.domain.diary.presentation.dto.req

import java.time.LocalDate

/**
 * 일기 수정 요청.
 *
 * null = 변경 없음.
 * [location] / [imageIds] 는 명시적으로 빈 값을 보내려면 빈 문자열 / 빈 리스트로 전달.
 */
data class UpdateDiaryRequest(
    val title: String? = null,
    val content: String? = null,
    val date: LocalDate? = null,
    val weather: Int? = null,
    val mood: Int? = null,
    val location: String? = null,
    /** null = 이미지 변경 없음. 빈 리스트 = 이미지 전부 제거. 값 있으면 그 순서대로 교체. */
    val imageIds: List<String>? = null
)
