package digdaserver.domain.character.application.service.impl

import digdaserver.domain.character.application.service.CharacterService
import digdaserver.domain.character.domain.entity.CharacterColor
import digdaserver.domain.character.domain.entity.CharacterStage
import digdaserver.domain.character.domain.entity.GroupCharacter
import digdaserver.domain.character.domain.entity.GroupCharacterColor
import digdaserver.domain.character.domain.repository.GroupCharacterColorRepository
import digdaserver.domain.character.domain.repository.GroupCharacterRepository
import digdaserver.domain.character.presentation.dto.res.AddExpResponse
import digdaserver.domain.character.presentation.dto.res.CharacterColorInfo
import digdaserver.domain.character.presentation.dto.res.CharacterColorShopResponse
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
    private val groupCharacterColorRepository: GroupCharacterColorRepository,
    private val groupRoomRepository: GroupRoomRepository,
    private val membershipRepository: MembershipRepository,
    @Lazy private val notificationService: NotificationService
) : CharacterService {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun getGroupCharacter(userId: UUID, groupRoomId: Long): CharacterStateResponse {
        validateGroupMember(groupRoomId, userId)
        val character = loadOrCreate(groupRoomId)
        log.info(
            "action=character_get, userId={}, groupRoomId={}, stage={}, level={}",
            userId,
            groupRoomId,
            character.stage,
            character.level
        )
        return CharacterStateResponse.from(character)
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

        return AddExpResponse.from(character, result, coinDelta)
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

    @Transactional
    override fun getColorShop(userId: UUID, groupRoomId: Long): CharacterColorShopResponse {
        validateGroupMember(groupRoomId, userId)
        val character = loadOrCreate(groupRoomId)
        val ownedColors =
            groupCharacterColorRepository.findAllByGroupRoomId(groupRoomId).map { it.color }.toSet()
        val items = CharacterColor.entries.map { color ->
            CharacterColorInfo(
                color = color,
                displayName = color.displayName,
                hex = color.hex,
                cost = color.cost,
                owned = color.isDefault || color in ownedColors,
                isCurrent = character.color == color,
                isDefault = color.isDefault
            )
        }
        return CharacterColorShopResponse(coin = character.coin, items = items)
    }

    @Transactional
    override fun buyColor(
        userId: UUID,
        groupRoomId: Long,
        color: CharacterColor
    ): CharacterColorShopResponse {
        validateGroupMember(groupRoomId, userId)
        val character = loadOrCreate(groupRoomId)

        if (color.isDefault) throw DigdaException(ErrorCode.ALREADY_OWNED_COLOR)
        if (groupCharacterColorRepository.existsByGroupRoomIdAndColor(groupRoomId, color)) {
            throw DigdaException(ErrorCode.ALREADY_OWNED_COLOR)
        }
        if (character.coin < color.cost) throw DigdaException(ErrorCode.INSUFFICIENT_COIN)

        character.deductCoin(color.cost)
        groupCharacterColorRepository.save(
            GroupCharacterColor(
                groupRoom = character.groupRoom,
                color = color,
                pricePaid = color.cost
            )
        )
        log.info(
            "action=character_buy_color, userId={}, groupRoomId={}, color={}, cost={}, balanceAfter={}",
            userId,
            groupRoomId,
            color,
            color.cost,
            character.coin
        )

        return getColorShop(userId, groupRoomId)
    }

    @Transactional
    override fun applyColor(
        userId: UUID,
        groupRoomId: Long,
        color: CharacterColor
    ): CharacterStateResponse {
        validateGroupMember(groupRoomId, userId)
        val character = loadOrCreate(groupRoomId)
        if (!color.isDefault &&
            !groupCharacterColorRepository.existsByGroupRoomIdAndColor(groupRoomId, color)
        ) {
            throw DigdaException(ErrorCode.COLOR_NOT_OWNED)
        }
        character.applyColor(color)
        log.info(
            "action=character_apply_color, userId={}, groupRoomId={}, color={}",
            userId,
            groupRoomId,
            color
        )
        return CharacterStateResponse.from(character)
    }

    private fun validateGroupMember(groupRoomId: Long, userId: UUID) {
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }
        if (groupRoom.deletedAt != null) throw DigdaException(ErrorCode.GROUP_ROOM_ALREADY_DELETED)
        membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }
    }

    private fun loadOrCreate(groupRoomId: Long): GroupCharacter {
        groupCharacterRepository.findByGroupRoomId(groupRoomId)?.let { return it }
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }
        val fresh = groupCharacterRepository.save(GroupCharacter(groupRoom = groupRoom))
        log.info(
            "action=character_create, groupRoomId={}, characterId={}",
            groupRoomId,
            fresh.id
        )
        return fresh
    }
}
