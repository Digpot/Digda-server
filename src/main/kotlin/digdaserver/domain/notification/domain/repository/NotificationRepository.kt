package digdaserver.domain.notification.domain.repository

import digdaserver.domain.notification.domain.entity.Notification
import digdaserver.domain.notification.domain.entity.NotificationType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface NotificationRepository : JpaRepository<Notification, Long> {

    fun findAllByUserIdOrderByCreatedAtDesc(userId: UUID, pageable: Pageable): Page<Notification>

    fun countByUserIdAndIsReadFalse(userId: UUID): Int

    /** 특정 일정(relatedId)에 대해 해당 종류의 알림이 이미 발송되었는지 확인 — 리마인더 중복 발송 방지. */
    fun existsByTypeAndRelatedId(type: NotificationType, relatedId: Long): Boolean

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    fun markAllAsReadByUserId(userId: UUID): Int

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.user.id = :userId")
    fun deleteAllByUserId(@Param("userId") userId: UUID)
}
