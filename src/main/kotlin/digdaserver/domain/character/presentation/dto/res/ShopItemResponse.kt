package digdaserver.domain.character.presentation.dto.res

import digdaserver.domain.character.domain.entity.ShopItem
import digdaserver.domain.character.domain.entity.ShopItemType

/**
 * 상점 아이템 단건 응답. 그룹의 보유/장착 여부는 컨텍스트별로 채워서 내려준다.
 */
data class ShopItemResponse(
    val itemKey: String,
    val itemType: ShopItemType,
    val itemTypeDisplayName: String,
    val displayName: String,
    val description: String?,
    val cost: Int,
    val assetKey: String,
    val accentColor: String?,
    val layerOrder: Int,
    val sortOrder: Int,
    val isDefault: Boolean,
    val owned: Boolean,
    val equipped: Boolean
) {
    companion object {
        fun from(item: ShopItem, owned: Boolean, equipped: Boolean): ShopItemResponse =
            ShopItemResponse(
                itemKey = item.itemKey,
                itemType = item.itemType,
                itemTypeDisplayName = item.itemType.displayName,
                displayName = item.displayName,
                description = item.description,
                cost = item.cost,
                assetKey = item.assetKey,
                accentColor = item.accentColor,
                layerOrder = item.layerOrder,
                sortOrder = item.sortOrder,
                isDefault = item.isDefault,
                owned = owned,
                equipped = equipped
            )
    }
}

/** 카테고리별 그룹화된 상점 응답. */
data class CharacterShopResponse(
    val coin: Int,
    val sections: List<ShopSection>
)

data class ShopSection(
    val itemType: ShopItemType,
    val itemTypeDisplayName: String,
    val slotOrder: Int,
    val items: List<ShopItemResponse>
)
