package digdaserver.domain.character.application.service.impl

import digdaserver.domain.character.domain.entity.GroupCharacter
import digdaserver.domain.character.domain.entity.GroupCharacterEquipped
import digdaserver.domain.character.domain.entity.GroupCharacterItem
import digdaserver.domain.character.domain.repository.GroupCharacterEquippedRepository
import digdaserver.domain.character.domain.repository.GroupCharacterItemRepository
import digdaserver.domain.character.domain.repository.ShopItemRepository
import org.springframework.stereotype.Component

/**
 * 그룹 캐릭터가 항상 default 아이템(스킨 등) 을 소유·장착하도록 보정.
 *
 * 메인/상점/퀴즈 등 캐릭터 로드 진입점이 여러 곳에 있어, 각 서비스가 이 컴포넌트를
 * 호출해 한 곳에서 일관되게 처리한다. idempotent — 이미 보유/장착된 경우 no-op.
 */
@Component
class CharacterGearInitializer(
    private val shopItemRepository: ShopItemRepository,
    private val groupCharacterItemRepository: GroupCharacterItemRepository,
    private val groupCharacterEquippedRepository: GroupCharacterEquippedRepository
) {

    fun ensureDefaults(character: GroupCharacter) {
        val groupRoomId = character.groupRoom.id
        val defaults = shopItemRepository.findAllDefaults()
        if (defaults.isEmpty()) return

        defaults.forEach { def ->
            if (!groupCharacterItemRepository
                    .existsByGroupRoomIdAndShopItemId(groupRoomId, def.id)
            ) {
                groupCharacterItemRepository.save(
                    GroupCharacterItem(
                        groupRoom = character.groupRoom,
                        shopItem = def,
                        pricePaid = 0
                    )
                )
            }
            val current = groupCharacterEquippedRepository
                .findByGroupRoomIdAndItemType(groupRoomId, def.itemType)
            if (current == null) {
                groupCharacterEquippedRepository.save(
                    GroupCharacterEquipped(
                        groupRoom = character.groupRoom,
                        itemType = def.itemType,
                        shopItem = def
                    )
                )
            }
        }
    }
}
