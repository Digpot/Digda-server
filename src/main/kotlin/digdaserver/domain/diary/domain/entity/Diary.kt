package digdaserver.domain.diary.domain.entity

import digdaserver.domain.group_room.domain.entity.GroupRoom
import digdaserver.domain.user.domain.entity.User
import digdaserver.global.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "diary")
class Diary(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "diary_id")
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_room_id", nullable = false)
    val groupRoom: GroupRoom,

    @Column(nullable = false, length = 20)
    var title: String,

    @Column(nullable = false, length = 300)
    var content: String,

    @Column(nullable = false)
    var date: LocalDate,

    @Column(nullable = false)
    var weather: Int,

    @Column(nullable = false)
    var mood: Int,

    @Column(name = "image_url")
    var imageUrl: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    val createdBy: User

) : BaseTimeEntity() {

    fun update(title: String?, content: String?, date: LocalDate?, weather: Int?, mood: Int?, imageUrl: String?) {
        title?.let { this.title = it }
        content?.let { this.content = it }
        date?.let { this.date = it }
        weather?.let { this.weather = it }
        mood?.let { this.mood = it }
        this.imageUrl = imageUrl
    }
}
