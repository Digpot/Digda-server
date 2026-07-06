package digdaserver.domain.schedule.presentation.dto.res

import digdaserver.domain.schedule.domain.entity.Schedule
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class ScheduleResponse(
    val id: Long,
    val title: String,
    val color: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val allDay: Boolean,
    val participants: List<UserSummary>,
    val createdBy: UserSummary,
    val commentCount: Int,
    val createdAt: LocalDateTime,
    /** 차단/신고로 숨겨진 일정인지. 목록에서는 보통 제외되며, 상세 직접 접근 방어용. */
    val hidden: Boolean = false,
    val hiddenReason: String? = null
) {
    fun asHidden(reason: String): ScheduleResponse = copy(
        title = "",
        hidden = true,
        hiddenReason = reason
    )

    companion object {
        fun from(schedule: Schedule, commentCount: Int): ScheduleResponse = ScheduleResponse(
            id = schedule.id,
            title = schedule.title,
            color = schedule.color,
            startDate = schedule.startDate,
            endDate = schedule.endDate,
            startTime = schedule.startTime,
            endTime = schedule.endTime,
            allDay = schedule.allDay,
            participants = schedule.participants.map { UserSummary.from(it.user) },
            createdBy = UserSummary.from(schedule.createdBy),
            commentCount = commentCount,
            createdAt = schedule.createdAt
        )
    }
}
