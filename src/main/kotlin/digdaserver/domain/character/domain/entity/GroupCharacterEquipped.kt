package digdaserver.domain.character.domain.entity

import digdaserver.domain.group_room.domain.entity.GroupRoom
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

/**
 * 그룹의 현재 장착 상태(카테고리당 1개).
 *
 * (group_room, item_type) 유니크라 같은 카테고리에서 다른 아이템을 장착하면 row 가
 * 갱신된다. 미착용 상태는 row 가 없는 것으로 표현 (`UNEQUIP` 은 row 삭제).
 *
 * SKIN 은 기본 1개가 항상 장착돼 있어야 렌더가 가능하다 — 해제 요청은 서비스 계층이
 * default 로 되돌리는 식으로 처리.
 */
@Entity
@Table(
    name = "group_character_equipped",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_gce_group_type",
            columnNames = ["group_room_id", "item_type"]
        )
    ]
)
class GroupCharacterEquipped(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_character_equipped_id")
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_room_id", nullable = false)
    val groupRoom: GroupRoom,

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 32)
    val itemType: ShopItemType,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_item_id", nullable = false)
    var shopItem: ShopItem,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun replaceWith(item: ShopItem) {
        require(item.itemType == this.itemType) {
            "item type mismatch: slot=${this.itemType}, item=${item.itemType}"
        }
        this.shopItem = item
        this.updatedAt = LocalDateTime.now()
    }
}
