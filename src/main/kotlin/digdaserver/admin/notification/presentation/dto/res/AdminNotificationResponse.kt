package digdaserver.admin.notification.presentation.dto.res

import digdaserver.domain.notification.domain.entity.Notification
import digdaserver.domain.notification.domain.entity.NotificationType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "어드민용 알림 행")
data class AdminNotificationResponse(

    @Schema(description = "알림 ID")
    val notificationId: Long,

    @Schema(description = "수신자 사용자 ID(UUID)")
    val recipientUserId: String,

    @Schema(description = "수신자 이름")
    val recipientName: String,

    @Schema(description = "알림 타입")
    val type: NotificationType,

    @Schema(description = "제목")
    val title: String,

    @Schema(description = "본문")
    val message: String,

    @Schema(description = "관련 그룹방 ID")
    val groupRoomId: Long?,

    @Schema(description = "관련 그룹방 이름")
    val groupRoomName: String?,

    @Schema(description = "관련 엔티티 ID")
    val relatedId: Long?,

    @Schema(description = "관련 엔티티 타입")
    val relatedType: String?,

    @Schema(description = "읽음 여부")
    val isRead: Boolean,

    @Schema(description = "생성 시각")
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(notification: Notification): AdminNotificationResponse =
            AdminNotificationResponse(
                notificationId = notification.id,
                recipientUserId = notification.user.id.toString(),
                recipientName = notification.user.name,
                type = notification.type,
                title = notification.title,
                message = notification.message,
                groupRoomId = notification.groupRoomId,
                groupRoomName = notification.groupRoomName,
                relatedId = notification.relatedId,
                relatedType = notification.relatedType,
                isRead = notification.isRead,
                createdAt = notification.createdAt
            )
    }
}
