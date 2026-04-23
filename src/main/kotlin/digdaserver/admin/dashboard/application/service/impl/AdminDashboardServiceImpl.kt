package digdaserver.admin.dashboard.application.service.impl

import digdaserver.admin.dashboard.application.service.AdminDashboardService
import digdaserver.admin.dashboard.presentation.dto.res.DashboardSummaryResponse
import digdaserver.domain.comment.domain.repository.CommentRepository
import digdaserver.domain.diary.domain.repository.DiaryRepository
import digdaserver.domain.group_room.domain.repository.GroupRoomRepository
import digdaserver.domain.notification.domain.repository.NotificationRepository
import digdaserver.domain.schedule.domain.repository.ScheduleRepository
import digdaserver.domain.todo.domain.repository.TodoRepository
import digdaserver.domain.user.domain.entity.Role
import digdaserver.domain.user.domain.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminDashboardServiceImpl(
    private val userRepository: UserRepository,
    private val groupRoomRepository: GroupRoomRepository,
    private val diaryRepository: DiaryRepository,
    private val scheduleRepository: ScheduleRepository,
    private val commentRepository: CommentRepository,
    private val todoRepository: TodoRepository,
    private val notificationRepository: NotificationRepository
) : AdminDashboardService {

    override fun getSummary(): DashboardSummaryResponse {
        return DashboardSummaryResponse(
            totalUsers = userRepository.count(),
            adminUsers = userRepository.countByRole(Role.ADMIN),
            totalGroupRooms = groupRoomRepository.count(),
            activeGroupRooms = groupRoomRepository.countByDeletedAtIsNull(),
            deleteScheduledGroupRooms = groupRoomRepository.countByDeleteScheduledAtIsNotNullAndDeletedAtIsNull(),
            totalDiaries = diaryRepository.count(),
            totalSchedules = scheduleRepository.count(),
            totalComments = commentRepository.count(),
            totalTodos = todoRepository.count(),
            totalNotifications = notificationRepository.count()
        )
    }
}
