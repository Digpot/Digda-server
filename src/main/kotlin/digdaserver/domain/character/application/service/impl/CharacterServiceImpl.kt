package digdaserver.domain.character.application.service.impl

import digdaserver.domain.character.application.service.CharacterService
import digdaserver.domain.character.domain.entity.CharacterStage
import digdaserver.domain.character.domain.entity.GroupCharacter
import digdaserver.domain.character.domain.entity.GroupCharacterEquipped
import digdaserver.domain.character.domain.entity.GroupCharacterItem
import digdaserver.domain.character.domain.repository.GroupCharacterEquippedRepository
import digdaserver.domain.character.domain.repository.GroupCharacterItemRepository
import digdaserver.domain.character.domain.repository.GroupCharacterRepository
import digdaserver.domain.character.domain.repository.ShopItemRepository
import digdaserver.domain.character.presentation.dto.res.AddExpResponse
import digdaserver.domain.character.presentation.dto.res.CharacterStageInfo
import digdaserver.domain.character.presentation.dto.res.CharacterStageTreeResponse
import digdaserver.domain.character.presentation.dto.res.CharacterStateResponse
import digdaserver.domain.group_room.domain.repository.GroupRoomRepository
import digdaserver.domain.membership.domain.repository.MembershipRepository
import digdaserver.domain.notification.application.service.NotificationService
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class CharacterServiceImpl(
    private val groupCharacterRepository: GroupCharacterRepository,
    private val groupCharacterItemRepository: GroupCharacterItemRepository,
    private val groupCharacterEquippedRepository: GroupCharacterEquippedRepository,
    private val shopItemRepository: ShopItemRepository,
    private val groupRoomRepository: GroupRoomRepository,
    private val membershipRepository: MembershipRepository,
    @Lazy private val notificationService: NotificationService
) : CharacterService {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun getGroupCharacter(userId: UUID, groupRoomId: Long): CharacterStateResponse {
        validateGroupMember(groupRoomId, userId)
        val character = loadOrCreate(groupRoomId)
        val equipped = groupCharacterEquippedRepository.findAllByGroupRoomId(groupRoomId)
        log.info(
            "action=character_get, userId={}, groupRoomId={}, stage={}, level={}",
            userId, groupRoomId, character.stage, character.level
        )
        return CharacterStateResponse.from(character, equipped)
    }

    @Transactional
    override fun gainExp(
        userId: UUID,
        groupRoomId: Long,
        amount: Int,
        coinDelta: Int,
        source: String?
    ): AddExpResponse {
        if (amount < 0) throw DigdaException(ErrorCode.INVALID_PARAMETER)
        if (coinDelta < 0) throw DigdaException(ErrorCode.INVALID_PARAMETER)

        validateGroupMember(groupRoomId, userId)
        val character = loadOrCreate(groupRoomId)
        val result = character.gainExp(amount)
        if (coinDelta > 0) character.addCoin(coinDelta)

        log.info(
            "action=character_gain_exp, userId={}, groupRoomId={}, amount={}, coinDelta={}, " +
                "source={}, level={}, stage={}, levelGained={}, stageChanged={}",
            userId, groupRoomId, amount, coinDelta, source,
            character.level, character.stage, result.levelGained, result.stageChanged
        )

        if (result.levelGained > 0 || result.stageChanged) {
            try {
                notificationService.notifyMochiLevelUp(
                    groupRoomId = groupRoomId,
                    actorUserId = userId,
                    newLevel = character.level,
                    stageChanged = result.stageChanged,
                    stageName = result.stageAfter.displayName
                )
            } catch (e: Exception) {
                log.warn(
                    "action=character_gain_exp_notify_failed, groupRoomId={}, error={}",
                    groupRoomId, e.message
                )
            }
        }

        val equipped = groupCharacterEquippedRepository.findAllByGroupRoomId(groupRoomId)
        return AddExpResponse.from(character, result, coinDelta, equipped)
    }

    @Transactional
    override fun getStageTree(userId: UUID, groupRoomId: Long): CharacterStageTreeResponse {
        validateGroupMember(groupRoomId, userId)
        val character = loadOrCreate(groupRoomId)
        val stages = CharacterStage.entries.map {
            CharacterStageInfo(
                stage = it,
                displayName = it.displayName,
                requiredLevel = it.requiredLevel,
                unlocked = character.level >= it.requiredLevel
            )
        }
        return CharacterStageTreeResponse(
            currentStage = character.stage,
            currentLevel = character.level,
            stages = stages
        )
    }

    private fun validateGroupMember(groupRoomId: Long, userId: UUID) {
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }
        if (groupRoom.deletedAt != null) throw DigdaException(ErrorCode.GROUP_ROOM_ALREADY_DELETED)
        membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }
    }

    private fun loadOrCreate(groupRoomId: Long): GroupCharacter {
        val existing = groupCharacterRepository.findByGroupRoomId(groupRoomId)
        if (existing != null) {
            ensureDefaultEquipped(existing)
            return existing
        }
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }
        val fresh = groupCharacterRepository.save(GroupCharacter(groupRoom = groupRoom))
        ensureDefaultEquipped(fresh)
        log.info("action=character_create, groupRoomId={}, characterId={}", groupRoomId, fresh.id)
        return fresh
    }

    /**
     * 캐릭터가 항상 default 아이템(코랄 스킨 등) 을 소유·장착하도록 보정.
     *
     * 신규 그룹뿐 아니라 마이그레이션 직후의 기존 그룹에서도 이 흐름을 타도록
     * 모든 진입점이 [loadOrCreate] → [ensureDefaultEquipped] 를 거치게 한다.
     */
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
}
