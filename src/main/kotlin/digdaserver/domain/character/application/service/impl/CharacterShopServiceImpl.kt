package digdaserver.domain.character.application.service.impl

import digdaserver.domain.character.application.service.CharacterShopService
import digdaserver.domain.character.domain.entity.GroupCharacter
import digdaserver.domain.character.domain.entity.GroupCharacterEquipped
import digdaserver.domain.character.domain.entity.GroupCharacterItem
import digdaserver.domain.character.domain.entity.ShopItem
import digdaserver.domain.character.domain.entity.ShopItemType
import digdaserver.domain.character.domain.repository.GroupCharacterEquippedRepository
import digdaserver.domain.character.domain.repository.GroupCharacterItemRepository
import digdaserver.domain.character.domain.repository.GroupCharacterRepository
import digdaserver.domain.character.domain.repository.ShopItemRepository
import digdaserver.domain.character.presentation.dto.res.CharacterShopResponse
import digdaserver.domain.character.presentation.dto.res.CharacterStateResponse
import digdaserver.domain.character.presentation.dto.res.ShopItemResponse
import digdaserver.domain.character.presentation.dto.res.ShopSection
import digdaserver.domain.group_room.domain.repository.GroupRoomRepository
import digdaserver.domain.membership.domain.repository.MembershipRepository
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 상점·인벤토리·장착 통합 서비스.
 *
 * 그룹 캐릭터는 첫 진입 시 lazy 생성되고, 그때 default 아이템(코랄 스킨)을 자동
 * 지급/장착해 항상 렌더 가능 상태를 유지한다. SKIN 슬롯의 default 보장 책임은
 * 이 서비스 한 곳에만 둔다 — 다른 도메인은 [GroupCharacterEquipped] 만 보고 그린다.
 */
@Service
@Transactional(readOnly = true)
class CharacterShopServiceImpl(
    private val shopItemRepository: ShopItemRepository,
    private val groupCharacterRepository: GroupCharacterRepository,
    private val groupCharacterItemRepository: GroupCharacterItemRepository,
    private val groupCharacterEquippedRepository: GroupCharacterEquippedRepository,
    private val groupRoomRepository: GroupRoomRepository,
    private val membershipRepository: MembershipRepository
) : CharacterShopService {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun getShop(userId: UUID, groupRoomId: Long): CharacterShopResponse {
        validateGroupMember(groupRoomId, userId)
        val character = loadOrInitCharacter(groupRoomId)

        val allItems = shopItemRepository
            .findAllByEnabledTrueOrderByItemTypeAscSortOrderAscShopItemIdAsc()
        val ownedIds = groupCharacterItemRepository.findAllByGroupRoomId(groupRoomId)
            .map { it.shopItem.id }
            .toSet()
        val equippedIds = groupCharacterEquippedRepository.findAllByGroupRoomId(groupRoomId)
            .map { it.shopItem.id }
            .toSet()

        val sections = allItems
            .groupBy { it.itemType }
            .toSortedMap(compareBy { it.slotOrder })
            .map { (type, items) ->
                ShopSection(
                    itemType = type,
                    itemTypeDisplayName = type.displayName,
                    slotOrder = type.slotOrder,
                    items = items.map {
                        ShopItemResponse.from(
                            item = it,
                            owned = it.isDefault || it.id in ownedIds,
                            equipped = it.id in equippedIds
                        )
                    }
                )
            }

        return CharacterShopResponse(coin = character.coin, sections = sections)
    }

    @Transactional
    override fun buyItem(
        userId: UUID,
        groupRoomId: Long,
        itemKey: String
    ): CharacterShopResponse {
        validateGroupMember(groupRoomId, userId)
        val character = loadOrInitCharacter(groupRoomId)
        val item = shopItemRepository.findByItemKey(itemKey)
            ?: throw DigdaException(ErrorCode.SHOP_ITEM_NOT_FOUND)
        if (!item.enabled) throw DigdaException(ErrorCode.SHOP_ITEM_NOT_FOUND)

        if (item.isDefault) throw DigdaException(ErrorCode.ALREADY_OWNED_ITEM)
        if (groupCharacterItemRepository.existsByGroupRoomIdAndShopItemId(groupRoomId, item.id)) {
            throw DigdaException(ErrorCode.ALREADY_OWNED_ITEM)
        }
        if (character.coin < item.cost) throw DigdaException(ErrorCode.INSUFFICIENT_COIN)

        character.deductCoin(item.cost)
        groupCharacterItemRepository.save(
            GroupCharacterItem(
                groupRoom = character.groupRoom,
                shopItem = item,
                pricePaid = item.cost
            )
        )

        log.info(
            "action=character_shop_buy_item, userId={}, groupRoomId={}, itemKey={}, cost={}, balanceAfter={}",
            userId, groupRoomId, itemKey, item.cost, character.coin
        )

        return getShop(userId, groupRoomId)
    }

    @Transactional
    override fun equipItem(
        userId: UUID,
        groupRoomId: Long,
        itemKey: String
    ): CharacterStateResponse {
        validateGroupMember(groupRoomId, userId)
        val character = loadOrInitCharacter(groupRoomId)
        val item = shopItemRepository.findByItemKey(itemKey)
            ?: throw DigdaException(ErrorCode.SHOP_ITEM_NOT_FOUND)
        if (!item.enabled) throw DigdaException(ErrorCode.SHOP_ITEM_NOT_FOUND)

        val owned = item.isDefault ||
            groupCharacterItemRepository.existsByGroupRoomIdAndShopItemId(groupRoomId, item.id)
        if (!owned) throw DigdaException(ErrorCode.ITEM_NOT_OWNED)

        upsertEquipped(character, item)

        log.info(
            "action=character_shop_equip, userId={}, groupRoomId={}, itemKey={}, itemType={}",
            userId, groupRoomId, itemKey, item.itemType
        )

        val equipped = groupCharacterEquippedRepository.findAllByGroupRoomId(groupRoomId)
        return CharacterStateResponse.from(character, equipped)
    }

    @Transactional
    override fun unequipSlot(
        userId: UUID,
        groupRoomId: Long,
        itemType: ShopItemType
    ): CharacterStateResponse {
        validateGroupMember(groupRoomId, userId)
        val character = loadOrInitCharacter(groupRoomId)

        if (itemType == ShopItemType.SKIN) {
            // 스킨은 렌더 필수 — default 로 복귀
            val default = shopItemRepository.findFirstByItemTypeAndIsDefaultTrue(itemType)
                ?: throw DigdaException(ErrorCode.SHOP_ITEM_NOT_FOUND)
            ensureOwned(groupRoomId, default)
            upsertEquipped(character, default)
        } else {
            groupCharacterEquippedRepository.deleteByGroupRoomIdAndItemType(groupRoomId, itemType)
        }

        log.info(
            "action=character_shop_unequip, userId={}, groupRoomId={}, itemType={}",
            userId, groupRoomId, itemType
        )

        val equipped = groupCharacterEquippedRepository.findAllByGroupRoomId(groupRoomId)
        return CharacterStateResponse.from(character, equipped)
    }

    // ── 내부 헬퍼 ─────────────────────────────────────────────────────

    private fun validateGroupMember(groupRoomId: Long, userId: UUID) {
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }
        if (groupRoom.deletedAt != null) throw DigdaException(ErrorCode.GROUP_ROOM_ALREADY_DELETED)
        membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }
    }

    private fun loadOrInitCharacter(groupRoomId: Long): GroupCharacter {
        val existing = groupCharacterRepository.findByGroupRoomId(groupRoomId)
        if (existing != null) {
            ensureDefaultEquipped(existing)
            return existing
        }
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }
        val fresh = groupCharacterRepository.save(GroupCharacter(groupRoom = groupRoom))
        ensureDefaultEquipped(fresh)
        log.info("action=character_create_via_shop, groupRoomId={}, characterId={}", groupRoomId, fresh.id)
        return fresh
    }

    /** default 아이템을 보유/장착에 추가 (idempotent). 스킨 슬롯이 빈 경우만 자동 적용. */
    private fun ensureDefaultEquipped(character: GroupCharacter) {
        val groupRoomId = character.groupRoom.id
        val defaults = shopItemRepository.findAllByIsDefaultTrue()
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
            val current =
                groupCharacterEquippedRepository.findByGroupRoomIdAndItemType(groupRoomId, def.itemType)
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

    private fun ensureOwned(groupRoomId: Long, item: ShopItem) {
        if (item.isDefault) return
        if (!groupCharacterItemRepository.existsByGroupRoomIdAndShopItemId(groupRoomId, item.id)) {
            throw DigdaException(ErrorCode.ITEM_NOT_OWNED)
        }
    }

    private fun upsertEquipped(character: GroupCharacter, item: ShopItem) {
        val groupRoomId = character.groupRoom.id
        val existing =
            groupCharacterEquippedRepository.findByGroupRoomIdAndItemType(groupRoomId, item.itemType)
        if (existing == null) {
            groupCharacterEquippedRepository.save(
                GroupCharacterEquipped(
                    groupRoom = character.groupRoom,
                    itemType = item.itemType,
                    shopItem = item
                )
            )
        } else {
            existing.apply(item)
        }
    }
}
