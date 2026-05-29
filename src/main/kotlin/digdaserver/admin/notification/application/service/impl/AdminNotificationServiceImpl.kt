package digdaserver.admin.notification.application.service.impl

import digdaserver.admin.common.dto.res.AdminPageResponse
import digdaserver.admin.notification.application.service.AdminNotificationService
import digdaserver.admin.notification.presentation.dto.res.AdminNotificationResponse
import digdaserver.domain.notification.domain.entity.NotificationType
import digdaserver.domain.notification.domain.repository.NotificationRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminNotificationServiceImpl(
    private val notificationRepository: NotificationRepository
) : AdminNotificationService {

    override fun search(
        types: Set<NotificationType>?,
        groupRoomId: Long?,
        keyword: String?,
        page: Int,
        size: Int
    ): AdminPageResponse<AdminNotificationResponse> {
        val safePage = page.coerceAtLeast(0)
        val safeSize = size.coerceIn(1, 100)
        val pageable = PageRequest.of(
            safePage,
            safeSize,
            Sort.by(Sort.Direction.DESC, "createdAt")
        )
        val safeTypes = types?.takeIf { it.isNotEmpty() }
        val result = notificationRepository.searchForAdmin(
            safeTypes,
            groupRoomId,
            keyword?.takeIf { it.isNotBlank() },
            pageable
        )
        return AdminPageResponse.of(result, AdminNotificationResponse::from)
    }
}
