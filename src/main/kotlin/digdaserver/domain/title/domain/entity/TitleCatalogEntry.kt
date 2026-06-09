package digdaserver.domain.title.domain.entity

import digdaserver.global.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * 칭호 카탈로그 1종(마스터 데이터). 부팅 시 [디그다 TitleCatalogSeeder] 가 idempotent 하게 시드하므로
 * 데이터를 날려도 `ddl-auto=create` + 시드로 재생성된다(모찌 ShopItem 과 동일 운영).
 *
 * 표시 메타(이름/색/아이콘)와 획득 조건을 모두 담아 어드민·앱이 [code] 로 공유한다.
 * - [category]       : region / diary / character (앱 분류 탭과 일치)
 * - [conditionType]  : region / diary / mochi_level / manual (획득 판정 종류)
 * - [conditionValue] : 조건 파라미터(지역 버킷명, 일기 임계값, 모찌 레벨). manual 은 null.
 * - [iconKey]        : 앱이 IconData 로 매핑하는 키.
 */
@Entity
@Table(
    name = "title_catalog",
    uniqueConstraints = [UniqueConstraint(name = "uk_title_catalog_code", columnNames = ["code"])]
)
class TitleCatalogEntry(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "title_catalog_id")
    val id: Long = 0L,

    @Column(nullable = false, length = 40)
    val code: String,

    @Column(nullable = false, length = 60)
    var name: String,

    @Column(nullable = false, length = 200)
    var description: String,

    @Column(nullable = false, length = 20)
    var category: String,

    @Column(name = "accent_color", nullable = false, length = 16)
    var accentColor: String,

    @Column(name = "icon_key", nullable = false, length = 40)
    var iconKey: String,

    @Column(name = "condition_type", nullable = false, length = 20)
    var conditionType: String,

    @Column(name = "condition_value", length = 40)
    var conditionValue: String? = null,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0

) : BaseTimeEntity() {

    fun updateMeta(
        name: String,
        description: String,
        category: String,
        accentColor: String,
        iconKey: String,
        conditionType: String,
        conditionValue: String?,
        sortOrder: Int
    ) {
        this.name = name
        this.description = description
        this.category = category
        this.accentColor = accentColor
        this.iconKey = iconKey
        this.conditionType = conditionType
        this.conditionValue = conditionValue
        this.sortOrder = sortOrder
    }
}
