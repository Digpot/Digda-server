package digdaserver.domain.diary.presentation.dto.req

import java.time.LocalDate

data class UpdateDiaryRequest(
    val title: String? = null,
    val content: String? = null,
    val date: LocalDate? = null,
    val weather: Int? = null,
    val mood: Int? = null,
    val imageId: String? = null
)
