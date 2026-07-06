package digdaserver.domain.log.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.UUID

@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(
    name = "user_action_log",
    indexes = [
        Index(name = "idx_user_log_created_at", columnList = "created_at"),
        Index(name = "idx_user_log_action", columnList = "action"),
        Index(name = "idx_user_log_actor", columnList = "actor_id")
    ]
)
class UserActionLog(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_action_log_id")
    val id: Long = 0L,

    @Column(name = "actor_id", columnDefinition = "BINARY(16)")
    val actorId: UUID?,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    val action: UserAction,

    @Column(name = "target_type", length = 40)
    val targetType: String?,

    @Column(name = "target_id", length = 100)
    val targetId: String?,

    @Column(length = 500)
    val detail: String?

) {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
        protected set
}
