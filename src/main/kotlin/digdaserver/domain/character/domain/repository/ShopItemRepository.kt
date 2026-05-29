package digdaserver.domain.character.domain.repository

import digdaserver.domain.character.domain.entity.ShopItem
import digdaserver.domain.character.domain.entity.ShopItemType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ShopItemRepository : JpaRepository<ShopItem, Long> {

    fun findByItemKey(itemKey: String): ShopItem?

    fun findAllByEnabledTrueOrderByItemTypeAscSortOrderAscShopItemIdAsc(): List<ShopItem>

    fun findAllByIsDefaultTrue(): List<ShopItem>

    fun findFirstByItemTypeAndIsDefaultTrue(itemType: ShopItemType): ShopItem?
}
