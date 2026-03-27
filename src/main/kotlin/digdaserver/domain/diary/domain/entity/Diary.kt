package digdaserver.domain.diary.domain.entity

import digdaserver.domain.group.domain.entity.GroupRoom
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
import jakarta.persistence.OrderBy
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    val createdBy: User

) : BaseTimeEntity() {

    @OneToMany(mappedBy = "diary", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    val images: MutableList<DiaryImage> = mutableListOf()

    fun update(title: String?, content: String?, date: LocalDate?, weather: Int?, mood: Int?) {
        title?.let { this.title = it }
        content?.let { this.content = it }
        date?.let { this.date = it }
        weather?.let { this.weather = it }
        mood?.let { this.mood = it }
    }

    fun clearImages() {
        this.images.clear()
    }

    fun addImage(image: DiaryImage) {
        this.images.add(image)
    }

    val thumbnailImage: String?
        get() = images.firstOrNull()?.imageUrl
}
