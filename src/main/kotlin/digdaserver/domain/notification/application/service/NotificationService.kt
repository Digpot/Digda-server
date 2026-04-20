package digdaserver.domain.notification.application.service

import digdaserver.domain.notification.presentation.dto.res.NotificationListResponse
import java.util.UUID

interface NotificationService {

    fun getNotifications(userId: UUID, limit: Int, offset: Int): NotificationListResponse

    fun markAsRead(userId: UUID, notificationId: Long, isRead: Boolean)

    fun markAllAsRead(userId: UUID)

    fun deleteNotification(userId: UUID, notificationId: Long)
}
