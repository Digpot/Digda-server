package digdaserver.domain.character.application.service.impl

import digdaserver.domain.character.application.service.CharacterService
import digdaserver.domain.character.domain.entity.CharacterStage
import digdaserver.domain.character.domain.entity.GroupCharacter
import digdaserver.domain.character.domain.repository.GroupCharacterEquippedRepository
import digdaserver.domain.character.domain.repository.GroupCharacterRepository
import digdaserver.domain.character.presentation.dto.res.AddExpResponse
import digdaserver.domain.character.presentation.dto.res.CharacterStageInfo
import digdaserver.domain.character.presentation.dto.res.CharacterStageTreeResponse
import digdaserver.domain.character.presentation.dto.res.CharacterStateResponse
import digdaserver.domain.character.presentation.dto.res.MasterGameRewardResponse
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
    private val groupCharacterEquippedRepository: GroupCharacterEquippedRepository,
    private val gearInitializer: CharacterGearInitializer,
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
            userId,
            groupRoomId,
            character.stage,
            character.level
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
                    groupRoomId,
                    e.message
                )
            }
        }

        if (result.dikoJustUnlocked) {
            try {
                notificationService.notifyDikoUnlocked(
                    groupRoomId = groupRoomId,
                    actorUserId = userId
                )
            } catch (e: Exception) {
                log.warn(
                    "action=character_gain_exp_diko_notify_failed, groupRoomId={}, error={}",
                    groupRoomId,
                    e.message
                )
            }
        }

        val equipped = groupCharacterEquippedRepository.findAllByGroupRoomId(groupRoomId)
        return AddExpResponse.from(character, result, coinDelta, equipped)
    }

    @Transactional
    override fun startMasterGame(userId: UUID, groupRoomId: Long): CharacterStateResponse {
        validateGroupMember(groupRoomId, userId)
        val character = loadOrCreate(groupRoomId)

        // 레벨 20(MAX) 부터 챔피언 챌린지를 응시할 수 있다. 마스터 진화 전(GLOW)에는
        // 이 게임이 "진화 시험" 으로, 진화 후(MASTER)에는 코인 파밍 콘텐츠로 동작한다.
        if (!character.isMaxLevel()) {
            throw DigdaException(ErrorCode.NOT_MASTER_CHARACTER)
        }
        if (character.coin < MASTER_GAME_ENTRY_FEE) {
            throw DigdaException(ErrorCode.INSUFFICIENT_COIN)
        }
        character.deductCoin(MASTER_GAME_ENTRY_FEE)

        log.info(
            "action=character_master_game_start, userId={}, groupRoomId={}, fee={}, balanceAfter={}",
            userId,
            groupRoomId,
            MASTER_GAME_ENTRY_FEE,
            character.coin
        )

        val equipped = groupCharacterEquippedRepository.findAllByGroupRoomId(groupRoomId)
        return CharacterStateResponse.from(character, equipped)
    }

    @Transactional
    override fun claimMasterGameReward(
        userId: UUID,
        groupRoomId: Long,
        score: Int
    ): MasterGameRewardResponse {
        if (score < 0 || score > MASTER_GAME_MAX_SCORE) {
            throw DigdaException(ErrorCode.INVALID_GAME_SCORE)
        }
        validateGroupMember(groupRoomId, userId)
        val character = loadOrCreate(groupRoomId)

        if (!character.isMaxLevel()) {
            throw DigdaException(ErrorCode.NOT_MASTER_CHARACTER)
        }

        val coinReward = rewardForScore(score)
        val tier = tierForScore(score)
        if (coinReward > 0) character.addCoin(coinReward)

        // 진화 시험: 아직 마스터가 아니고 "훌륭" 이상(score>=MASTER_EVOLVE_MIN_SCORE) 이면
        // 이번 도전으로 마스터 진화. 이미 마스터면 코인만 적립.
        val evolvedToMaster =
            if (!character.masterUnlocked && score >= MASTER_EVOLVE_MIN_SCORE) {
                character.evolveToMaster()
            } else {
                false
            }

        log.info(
            "action=character_master_game_reward, userId={}, groupRoomId={}, score={}, " +
                "tier={}, coinReward={}, evolvedToMaster={}, balanceAfter={}",
            userId,
            groupRoomId,
            score,
            tier,
            coinReward,
            evolvedToMaster,
            character.coin
        )

        if (evolvedToMaster) {
            try {
                notificationService.notifyMochiLevelUp(
                    groupRoomId = groupRoomId,
                    actorUserId = userId,
                    newLevel = character.level,
                    stageChanged = true,
                    stageName = CharacterStage.MASTER.displayName
                )
            } catch (e: Exception) {
                log.warn(
                    "action=character_master_evolve_notify_failed, groupRoomId={}, error={}",
                    groupRoomId,
                    e.message
                )
            }
        }

        val equipped = groupCharacterEquippedRepository.findAllByGroupRoomId(groupRoomId)
        return MasterGameRewardResponse(
            score = score,
            coinReward = coinReward,
            tier = tier,
            evolvedToMaster = evolvedToMaster,
            character = CharacterStateResponse.from(character, equipped)
        )
    }

    private fun rewardForScore(score: Int): Int = when {
        score >= 16 -> 200
        score >= 11 -> 100
        score >= 6 -> 50
        score >= 1 -> 20
        else -> 0
    }

    private fun tierForScore(score: Int): String = when {
        score >= 16 -> "전설"
        score >= 11 -> "훌륭"
        score >= 6 -> "우수"
        score >= 1 -> "도전"
        else -> "참여"
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

    companion object {
        /** 30 초 × 약 0.8 s 등장 주기 + 추가 마진 — 비현실적 점수 차단. */
        private const val MASTER_GAME_MAX_SCORE = 60

        /** 마스터 게임 입장료 (코인). */
        private const val MASTER_GAME_ENTRY_FEE = 20

        /** 마스터 진화 시험 통과 최소 점수 — "훌륭"(11점) 이상. */
        private const val MASTER_EVOLVE_MIN_SCORE = 11
    }

    private fun loadOrCreate(groupRoomId: Long): GroupCharacter {
        val existing = groupCharacterRepository.findByGroupRoomId(groupRoomId)
        if (existing != null) {
            gearInitializer.ensureDefaults(existing)
            return existing
        }
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }
        val fresh = groupCharacterRepository.save(GroupCharacter(groupRoom = groupRoom))
        gearInitializer.ensureDefaults(fresh)
        log.info("action=character_create, groupRoomId={}, characterId={}", groupRoomId, fresh.id)
        return fresh
    }
}
