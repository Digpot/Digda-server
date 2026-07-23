package digdaserver.domain.notification.domain.entity

import digdaserver.domain.user.domain.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "notification",
    indexes = [
        Index(name = "idx_notification_user", columnList = "user_id"),
        Index(name = "idx_notification_user_read", columnList = "user_id, is_read"),
        Index(name = "idx_notification_created_at", columnList = "created_at")
    ]
)
class Notification(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    // MySQL 네이티브 ENUM 생성을 막고 VARCHAR 로 고정 — SchemaAutoMigration 의 prod 정정과 일치.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(64)")
    val type: NotificationType,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false)
    val message: String,

    @Column(name = "group_room_id")
    val groupRoomId: Long? = null,

    @Column(name = "group_room_name")
    val groupRoomName: String? = null,

    @Column(name = "related_id")
    val relatedId: Long? = null,

    @Column(name = "related_type")
    val relatedType: String? = null,

    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {

    fun markAsRead() {
        this.isRead = true
    }
}
