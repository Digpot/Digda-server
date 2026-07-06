package digdaserver.admin.notification.application.service

import digdaserver.admin.common.dto.res.AdminPageResponse
import digdaserver.admin.notification.presentation.dto.res.AdminNotificationResponse
import digdaserver.domain.notification.domain.entity.NotificationType

interface AdminNotificationService {

    fun search(
        types: Set<NotificationType>?,
        groupRoomId: Long?,
        keyword: String?,
        page: Int,
        size: Int
    ): AdminPageResponse<AdminNotificationResponse>
}
