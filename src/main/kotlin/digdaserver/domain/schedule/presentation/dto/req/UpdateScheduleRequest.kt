package digdaserver.domain.schedule.presentation.dto.req

import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class UpdateScheduleRequest(
    val title: String? = null,
    val color: String? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val allDay: Boolean? = null,
    val participantIds: List<UUID>? = null
)
