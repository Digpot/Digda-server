package digdaserver.admin.dashboard.presentation.dto.res

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "어드민 대시보드 주요 통계")
data class DashboardSummaryResponse(

    @Schema(description = "총 사용자 수")
    val totalUsers: Long,

    @Schema(description = "관리자 수")
    val adminUsers: Long,

    @Schema(description = "총 그룹방 수")
    val totalGroupRooms: Long,

    @Schema(description = "활성 그룹방 수 (삭제되지 않음)")
    val activeGroupRooms: Long,

    @Schema(description = "삭제 예약된 그룹방 수")
    val deleteScheduledGroupRooms: Long,

    @Schema(description = "총 일기 수")
    val totalDiaries: Long,

    @Schema(description = "총 일정 수")
    val totalSchedules: Long,

    @Schema(description = "총 댓글 수")
    val totalComments: Long,

    @Schema(description = "총 할 일 수")
    val totalTodos: Long,

    @Schema(description = "총 알림 수")
    val totalNotifications: Long
)
