package digdaserver.admin.schedule.presentation.dto.res

import digdaserver.domain.schedule.domain.entity.Schedule
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Schema(description = "어드민용 일정 정보")
data class AdminScheduleResponse(

    @Schema(description = "일정 ID")
    val scheduleId: Long,

    @Schema(description = "그룹방 ID")
    val groupRoomId: Long,

    @Schema(description = "그룹방 이름")
    val groupRoomName: String,

    @Schema(description = "작성자 ID(UUID)")
    val createdBy: String,

    @Schema(description = "작성자 이름")
    val authorName: String,

    @Schema(description = "제목")
    val title: String,

    @Schema(description = "색상(#RRGGBB)")
    val color: String,

    @Schema(description = "시작 일자")
    val startDate: LocalDate,

    @Schema(description = "종료 일자")
    val endDate: LocalDate,

    @Schema(description = "시작 시간")
    val startTime: LocalTime?,

    @Schema(description = "종료 시간")
    val endTime: LocalTime?,

    @Schema(description = "종일 여부")
    val allDay: Boolean,

    @Schema(description = "참여자 수")
    val participantCount: Int,

    @Schema(description = "생성 시각")
    val createdAt: LocalDateTime,

    @Schema(description = "수정 시각")
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(schedule: Schedule): AdminScheduleResponse = AdminScheduleResponse(
            scheduleId = schedule.id,
            groupRoomId = schedule.groupRoom.id,
            groupRoomName = schedule.groupRoom.name,
            createdBy = schedule.createdBy.id.toString(),
            authorName = schedule.createdBy.name,
            title = schedule.title,
            color = schedule.color,
            startDate = schedule.startDate,
            endDate = schedule.endDate,
            startTime = schedule.startTime,
            endTime = schedule.endTime,
            allDay = schedule.allDay,
            participantCount = schedule.participants.size,
            createdAt = schedule.createdAt,
            updatedAt = schedule.updatedAt
        )
    }
}
