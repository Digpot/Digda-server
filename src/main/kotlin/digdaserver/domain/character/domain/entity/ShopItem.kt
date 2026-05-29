package digdaserver.domain.character.domain.entity

import digdaserver.global.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * 상점 아이템 마스터(전 그룹 공통).
 *
 * 시드 데이터는 [digdaserver.domain.character.infra.ShopItemSeeder] 가 부팅 시
 * idempotent 하게 적용한다. 동일 [itemKey] 가 존재하면 메타만 갱신, 없으면 새로 추가.
 *
 * 클라이언트는 [itemKey] 로 아이템을 식별하고, [assetKey] / [accentColor] 를 보고
 * 렌더한다. asset 이 새로 추가될 때마다 앱 배포가 필요하지만, 가격/표시명/노출 여부는
 * 서버에서 즉시 조정 가능하게 분리했다.
 */
@Entity
@Table(
    name = "shop_item",
    uniqueConstraints = [UniqueConstraint(name = "uq_shop_item_key", columnNames = ["item_key"])],
    indexes = [Index(name = "idx_shop_item_type_sort", columnList = "item_type, sort_order")]
)
class ShopItem(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shop_item_id")
    val id: Long = 0L,

    @Column(name = "item_key", nullable = false, length = 64)
    val itemKey: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 32)
    val itemType: ShopItemType,

    @Column(name = "display_name", nullable = false, length = 64)
    var displayName: String,

    @Column(name = "description", length = 255)
    var description: String? = null,

    /** 코인 가격. 기본(=default) 은 0. */
    @Column(name = "cost", nullable = false)
    var cost: Int = 0,

    /** 렌더러용 asset 식별자 (예: "skin/coral", "item/glasses_round"). */
    @Column(name = "asset_key", nullable = false, length = 128)
    var assetKey: String,

    /** SKIN 류는 배경 squircle 의 hex. 액세서리는 nullable. */
    @Column(name = "accent_color", length = 16)
    var accentColor: String? = null,

    /** 렌더 시 z-order. 큰 값이 위로 와야 모자/안경이 머리 위에 그려진다. */
    @Column(name = "layer_order", nullable = false)
    var layerOrder: Int = 0,

    /** 같은 카테고리 안에서의 표시 정렬. */
    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,

    /** true 면 신규 그룹에 자동 지급 + 장착됨. */
    @Column(name = "is_default", nullable = false)
    var isDefault: Boolean = false,

    /** false 면 상점에서 숨김(이미 보유한 그룹의 장착은 유지). */
    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = true
) : BaseTimeEntity() {

    fun updateMeta(
        displayName: String,
        description: String?,
        cost: Int,
        assetKey: String,
        accentColor: String?,
        layerOrder: Int,
        sortOrder: Int,
        isDefault: Boolean,
        enabled: Boolean
    ) {
        this.displayName = displayName
        this.description = description
        this.cost = cost
        this.assetKey = assetKey
        this.accentColor = accentColor
        this.layerOrder = layerOrder
        this.sortOrder = sortOrder
        this.isDefault = isDefault
        this.enabled = enabled
    }
}
