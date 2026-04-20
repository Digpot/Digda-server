package digdaserver.domain.notification.application.service.impl

import digdaserver.domain.notification.application.service.NotificationService
import digdaserver.domain.notification.domain.repository.NotificationRepository
import digdaserver.domain.notification.presentation.dto.res.NotificationListResponse
import digdaserver.domain.notification.presentation.dto.res.NotificationResponse
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class NotificationServiceImpl(
    private val notificationRepository: NotificationRepository
) : NotificationService {

    override fun getNotifications(userId: UUID, limit: Int, offset: Int): NotificationListResponse {
        val safeLimit = limit.coerceIn(1, 100)
        val safeOffset = offset.coerceAtLeast(0)
        val pageable = PageRequest.of(safeOffset / safeLimit, safeLimit)

        val page = notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable)
        val unreadCount = notificationRepository.countByUserIdAndIsReadFalse(userId)

        return NotificationListResponse(
            notifications = page.content.map { NotificationResponse.from(it) },
            total = page.totalElements,
            unreadCount = unreadCount,
            limit = safeLimit,
            offset = safeOffset,
            hasMore = (safeOffset + page.content.size) < page.totalElements
        )
    }

    @Transactional
    override fun markAsRead(userId: UUID, notificationId: Long, isRead: Boolean) {
        val notification = notificationRepository.findById(notificationId)
            .orElseThrow { DigdaException(ErrorCode.NOTIFICATION_NOT_FOUND) }

        if (notification.user.id != userId) {
            throw DigdaException(ErrorCode.FORBIDDEN)
        }

        if (isRead) {
            notification.markAsRead()
        }
    }

    @Transactional
    override fun markAllAsRead(userId: UUID) {
        notificationRepository.markAllAsReadByUserId(userId)
    }

    @Transactional
    override fun deleteNotification(userId: UUID, notificationId: Long) {
        val notification = notificationRepository.findById(notificationId)
            .orElseThrow { DigdaException(ErrorCode.NOTIFICATION_NOT_FOUND) }

        if (notification.user.id != userId) {
            throw DigdaException(ErrorCode.FORBIDDEN)
        }

        notificationRepository.delete(notification)
    }
}
