package digdaserver.domain.todo.domain.entity

import digdaserver.domain.group_room.domain.entity.GroupRoom
import digdaserver.domain.user.domain.entity.User
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
@Table(name = "todo")
class Todo(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "todo_id")
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_room_id", nullable = false)
    val groupRoom: GroupRoom,

    @Column(nullable = false, length = 100)
    val text: String,

    @Column(nullable = false)
    var completed: Boolean = false,

    @Column(name = "completed_at")
    var completedAt: LocalDateTime? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by")
    var completedBy: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    val createdBy: User,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {

    fun toggleComplete(user: User) {
        if (completed) {
            this.completed = false
            this.completedAt = null
            this.completedBy = null
        } else {
            this.completed = true
            this.completedAt = LocalDateTime.now()
            this.completedBy = user
        }
    }
}
