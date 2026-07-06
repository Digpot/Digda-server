package digdaserver.domain.group_room.presentation.dto.res

import digdaserver.domain.schedule.domain.entity.Schedule
import java.time.LocalDate
import java.time.LocalTime

/**
 * 그룹 홈(대시보드) 화면 1회 조회용 집계 응답.
 *
 * - [userName]    : 인사 헤더용 사용자 이름
 * - [today]       : 오늘 요약 스트립(오늘 일정 수 / 오늘 새 일기 수 / 안읽음 알림 수)
 * - [activeGroup] : 활성 그룹 카드(멤버 + 다가오는 일정 내장)
 *
 * 최근 소식(activity) 피드는 별도 `/notifications` 엔드포인트(cross-group)를 재사용하므로
 * 이 응답에는 포함하지 않는다.
 */
data class GroupHomeResponse(
    val userName: String,
    val today: TodaySummaryResponse,
    val activeGroup: ActiveGroupResponse
)

data class TodaySummaryResponse(
    val scheduleCount: Int,
    val newDiaryCount: Int,
    val unreadCount: Int
)

data class ActiveGroupResponse(
    val id: Long,
    val name: String,
    val thumbnailImage: String?,
    val memberCount: Int,
    val myRole: String,
    val members: List<MembershipSummary>,
    val nextEvent: NextEventResponse?
)

data class NextEventResponse(
    val id: Long,
    val title: String,
    val startDate: LocalDate,
    val startTime: LocalTime?,
    val allDay: Boolean,
    val color: String
) {
    companion object {
        fun from(schedule: Schedule): NextEventResponse = NextEventResponse(
            id = schedule.id,
            title = schedule.title,
            startDate = schedule.startDate,
            startTime = schedule.startTime,
            allDay = schedule.allDay,
            color = schedule.color
        )
    }
}
