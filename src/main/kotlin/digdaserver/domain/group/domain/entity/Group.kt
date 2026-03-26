package digdaserver.domain.group.domain.entity

import digdaserver.domain.diary.domain.entity.Diary
import digdaserver.domain.invite.domain.entity.InviteCode
import digdaserver.domain.membership.domain.entity.Membership
import digdaserver.domain.schedule.domain.entity.Schedule
import digdaserver.domain.todo.domain.entity.Todo
import digdaserver.domain.user.domain.entity.User
import digdaserver.global.common.entity.BaseTimeEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "group_room")
class Group(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    val id: Long = 0L,

    @Column(nullable = false, length = 20)
    var name: String,

    @Column(name = "thumbnail_image")
    var thumbnailImage: String? = null,

    @Column(name = "max_members", nullable = false)
    var maxMembers: Int,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    var owner: User,

    @Column(name = "last_activity_at", nullable = false)
    var lastActivityAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "delete_scheduled_at")
    var deleteScheduledAt: LocalDateTime? = null,

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null

) : BaseTimeEntity() {

    @OneToMany(mappedBy = "group", cascade = [CascadeType.ALL], orphanRemoval = true)
    val memberships: MutableList<Membership> = mutableListOf()

    @OneToMany(mappedBy = "group", cascade = [CascadeType.ALL], orphanRemoval = true)
    val inviteCodes: MutableList<InviteCode> = mutableListOf()

    @OneToMany(mappedBy = "group", cascade = [CascadeType.ALL], orphanRemoval = true)
    val schedules: MutableList<Schedule> = mutableListOf()

    @OneToMany(mappedBy = "group", cascade = [CascadeType.ALL], orphanRemoval = true)
    val diaries: MutableList<Diary> = mutableListOf()

    @OneToMany(mappedBy = "group", cascade = [CascadeType.ALL], orphanRemoval = true)
    val todos: MutableList<Todo> = mutableListOf()

    val isDeleteScheduled: Boolean
        get() = deleteScheduledAt != null && deletedAt == null

    fun update(name: String?, maxMembers: Int?, thumbnailImage: String?) {
        name?.let { this.name = it }
        maxMembers?.let { this.maxMembers = it }
        thumbnailImage?.let { this.thumbnailImage = it }
    }

    fun removeThumbnail() {
        this.thumbnailImage = null
    }

    fun scheduleDelete() {
        this.deleteScheduledAt = LocalDateTime.now().plusDays(7)
    }

    fun recover() {
        this.deleteScheduledAt = null
    }

    fun markDeleted() {
        this.deletedAt = LocalDateTime.now()
    }

    fun updateLastActivity() {
        this.lastActivityAt = LocalDateTime.now()
    }

    fun transferOwnership(newOwner: User) {
        this.owner = newOwner
    }
}
