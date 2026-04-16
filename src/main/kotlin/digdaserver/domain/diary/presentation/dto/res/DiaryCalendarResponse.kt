package digdaserver.domain.diary.presentation.dto.res

import java.time.LocalDate

data class DiaryCalendarResponse(
    val dates: List<LocalDate>
)
