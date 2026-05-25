package digdaserver.domain.diary.domain.entity

import digdaserver.domain.group_room.domain.entity.GroupRoom
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

    @Column(name = "location", length = 100)
    var location: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    val createdBy: User,

    @OneToMany(mappedBy = "diary", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    val images: MutableList<DiaryImage> = mutableListOf()

) : BaseTimeEntity() {

    fun updateBasics(
        title: String?,
        content: String?,
        date: LocalDate?,
        weather: Int?,
        mood: Int?,
        location: String?
    ) {
        title?.let { this.title = it }
        content?.let { this.content = it }
        date?.let { this.date = it }
        weather?.let { this.weather = it }
        mood?.let { this.mood = it }
        this.location = location
    }

    /** 이미지를 전부 재구성. urls 순서대로 sort_order 0..N-1 부여. */
    fun replaceImages(urls: List<String>) {
        images.clear()
        urls.forEachIndexed { index, url ->
            images.add(DiaryImage(diary = this, url = url, sortOrder = index))
        }
    }
}
