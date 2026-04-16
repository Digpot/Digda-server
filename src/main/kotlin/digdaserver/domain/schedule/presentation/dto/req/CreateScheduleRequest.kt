package digdaserver.domain.schedule.presentation.dto.req

import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class CreateScheduleRequest(
    val title: String,
    val color: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val allDay: Boolean,
    val participantIds: List<UUID>? = null
)
