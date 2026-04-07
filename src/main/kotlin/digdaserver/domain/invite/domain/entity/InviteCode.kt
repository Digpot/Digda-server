package digdaserver.domain.invite.domain.entity

import digdaserver.domain.group.domain.entity.GroupRoom
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "invite_code")
class InviteCode(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invite_code_id")
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_room_id", nullable = false)
    val groupRoom: GroupRoom,

    @Column(nullable = false, unique = true, length = 6)
    val code: String,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: LocalDateTime,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {

    val isExpired: Boolean
        get() = LocalDateTime.now().isAfter(expiresAt)
}
