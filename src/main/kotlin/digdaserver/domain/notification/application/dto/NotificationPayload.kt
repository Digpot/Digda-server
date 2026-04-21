package digdaserver.domain.notification.application.dto

import digdaserver.domain.notification.domain.entity.NotificationType

data class NotificationPayload(
    val type: NotificationType,
    val title: String,
    val message: String,
    val groupRoomId: Long? = null,
    val groupRoomName: String? = null,
    val relatedId: Long? = null,
    val relatedType: String? = null
)
