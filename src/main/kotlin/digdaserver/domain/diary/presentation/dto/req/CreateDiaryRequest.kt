package digdaserver.domain.diary.presentation.dto.req

import java.time.LocalDate

data class CreateDiaryRequest(
    val title: String,
    val content: String,
    val date: LocalDate,
    val weather: Int,
    val mood: Int,
    val imageId: String? = null
)
