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

    /**
     * 시그니처 지도 색칠용 정규 지역 키. 앱이 색칠 분류의 단일 소스이므로 앱에서 산출해 보낸다.
     * 광역시·세종 = 시도명("인천"), 도 = 시·군명("남원시"; 통합시는 일반구 합산해 母市 "창원시").
     */
    @Column(name = "region_key", length = 40)
    var regionKey: String? = null,

    /** 표시용 시도명 (예: "전북특별자치도"). */
    @Column(name = "region_sido", length = 30)
    var regionSido: String? = null,

    /** 표시용 시군구명 (예: "남원시", "창원시 성산구"). */
    @Column(name = "region_sigungu", length = 60)
    var regionSigungu: String? = null,

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
        location: String?,
        regionKey: String?,
        regionSido: String?,
        regionSigungu: String?
    ) {
        title?.let { this.title = it }
        content?.let { this.content = it }
        date?.let { this.date = it }
        weather?.let { this.weather = it }
        mood?.let { this.mood = it }
        // location 과 지역은 항상 요청값으로 덮어쓴다(빈 값 = 위치 제거 의도).
        this.location = location
        this.regionKey = regionKey
        this.regionSido = regionSido
        this.regionSigungu = regionSigungu
    }

    /** 이미지를 전부 재구성. urls 순서대로 sort_order 0..N-1 부여. */
    fun replaceImages(urls: List<String>) {
        images.clear()
        urls.forEachIndexed { index, url ->
            images.add(DiaryImage(diary = this, url = url, sortOrder = index))
        }
    }
}
