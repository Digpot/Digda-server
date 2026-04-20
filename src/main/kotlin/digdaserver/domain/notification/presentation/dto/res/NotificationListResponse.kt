package digdaserver.domain.notification.presentation.dto.res

data class NotificationListResponse(
    val notifications: List<NotificationResponse>,
    val total: Long,
    val unreadCount: Int,
    val limit: Int,
    val offset: Int,
    val hasMore: Boolean
)
