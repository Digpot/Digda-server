package digdaserver.domain.schedule.presentation.dto.res

data class ScheduleDetailResponse(
    val schedule: ScheduleResponse,
    val comments: List<CommentResponse>
)
