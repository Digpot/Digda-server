package digdaserver.domain.diary.presentation.dto.req

import java.time.LocalDate

data class CreateDiaryRequest(
    val title: String,
    val content: String,
    val date: LocalDate,
    val weather: Int,
    val mood: Int,
    val location: String? = null,
    /** UploadedImage.id (Long) 의 문자열 리스트. 0..10 장. 순서대로 sort_order 부여. */
    val imageIds: List<String> = emptyList()
)
