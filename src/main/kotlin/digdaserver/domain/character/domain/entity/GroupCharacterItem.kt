package digdaserver.domain.character.domain.entity

import digdaserver.domain.group_room.domain.entity.GroupRoom
import jakarta.persistence.Column
import jakarta.persistence.Entity
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
 * 그룹별 보유 아이템.
 *
 * (group_room, shop_item) 유니크 — 같은 아이템 중복 구매는 [ALREADY_OWNED_ITEM] 으로
 * 거절된다. default 아이템은 그룹 첫 생성 시 자동 보유 처리.
 */
@Entity
@Table(
    name = "group_character_item",
    uniqueConstraints = [UniqueConstraint(
        name = "uq_group_character_item",
        columnNames = ["group_room_id", "shop_item_id"]
    )]
)
class GroupCharacterItem(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_character_item_id")
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_room_id", nullable = false)
    val groupRoom: GroupRoom,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_item_id", nullable = false)
    val shopItem: ShopItem,

    @Column(name = "price_paid", nullable = false)
    val pricePaid: Int,

    @Column(name = "acquired_at", nullable = false)
    val acquiredAt: LocalDateTime = LocalDateTime.now()
)
