package digdaserver.domain.character.presentation.dto.res

import digdaserver.domain.character.domain.entity.GroupCharacterEquipped
import digdaserver.domain.character.domain.entity.ShopItemType

/**
 * 캐릭터 외형 렌더에 필요한 최소 정보. 클라이언트는 [itemType] 별로 그룹화해
 * 슬롯 1개씩 그린다.
 */
data class EquippedItemResponse(
    val itemType: ShopItemType,
    val itemKey: String,
    val displayName: String,
    val assetKey: String,
    val accentColor: String?,
    val layerOrder: Int
) {
    companion object {
        fun from(eq: GroupCharacterEquipped): EquippedItemResponse =
            EquippedItemResponse(
                itemType = eq.itemType,
                itemKey = eq.shopItem.itemKey,
                displayName = eq.shopItem.displayName,
                assetKey = eq.shopItem.assetKey,
                accentColor = eq.shopItem.accentColor,
                layerOrder = eq.shopItem.layerOrder
            )
    }
}
