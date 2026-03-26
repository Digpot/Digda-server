package digdaserver.domain.membership.domain.entity

import digdaserver.domain.group.domain.entity.Group
import digdaserver.domain.group.domain.entity.GroupRole
import digdaserver.domain.user.domain.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "membership",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "group_id"])]
)
class Membership(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "membership_id")
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    val group: Group,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: GroupRole,

    @Column(nullable = false, length = 7)
    var color: String,

    @Column(name = "joined_at", nullable = false)
    val joinedAt: LocalDateTime = LocalDateTime.now()
) {

    fun changeRole(newRole: GroupRole) {
        this.role = newRole
    }

    val isOwner: Boolean
        get() = role == GroupRole.OWNER
}
