package digdaserver.domain.character.application.service.impl

import digdaserver.domain.character.application.service.CharacterService
import digdaserver.domain.character.domain.entity.CharacterColor
import digdaserver.domain.character.domain.entity.CharacterStage
import digdaserver.domain.character.domain.entity.UserCharacter
import digdaserver.domain.character.domain.entity.UserCharacterColor
import digdaserver.domain.character.domain.repository.UserCharacterColorRepository
import digdaserver.domain.character.domain.repository.UserCharacterRepository
import digdaserver.domain.character.presentation.dto.res.AddExpResponse
import digdaserver.domain.character.presentation.dto.res.CharacterColorInfo
import digdaserver.domain.character.presentation.dto.res.CharacterColorShopResponse
import digdaserver.domain.character.presentation.dto.res.CharacterStageInfo
import digdaserver.domain.character.presentation.dto.res.CharacterStageTreeResponse
import digdaserver.domain.character.presentation.dto.res.CharacterStateResponse
import digdaserver.domain.user.domain.repository.UserRepository
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class CharacterServiceImpl(
    private val userCharacterRepository: UserCharacterRepository,
    private val userCharacterColorRepository: UserCharacterColorRepository,
    private val userRepository: UserRepository
) : CharacterService {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun getMyCharacter(userId: UUID): CharacterStateResponse {
        val character = loadOrCreate(userId)
        log.info("action=character_get_me, userId={}, stage={}, level={}", userId, character.stage, character.level)
        return CharacterStateResponse.from(character)
    }

    @Transactional
    override fun gainExp(userId: UUID, amount: Int, coinDelta: Int, source: String?): AddExpResponse {
        if (amount < 0) throw DigdaException(ErrorCode.INVALID_PARAMETER)
        if (coinDelta < 0) throw DigdaException(ErrorCode.INVALID_PARAMETER)

        val character = loadOrCreate(userId)
        val result = character.gainExp(amount)
        if (coinDelta > 0) character.addCoin(coinDelta)

        log.info(
            "action=character_gain_exp, userId={}, amount={}, coinDelta={}, source={}, level={}, stage={}, levelGained={}, stageChanged={}",
            userId, amount, coinDelta, source, character.level, character.stage, result.levelGained, result.stageChanged
        )
        return AddExpResponse.from(character, result, coinDelta)
    }

    @Transactional
    override fun getStageTree(userId: UUID): CharacterStageTreeResponse {
        val character = loadOrCreate(userId)
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
    override fun getColorShop(userId: UUID): CharacterColorShopResponse {
        val character = loadOrCreate(userId)
        val ownedColors = userCharacterColorRepository.findAllByUserId(userId).map { it.color }.toSet()
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
    override fun buyColor(userId: UUID, color: CharacterColor): CharacterColorShopResponse {
        val character = loadOrCreate(userId)

        if (color.isDefault) throw DigdaException(ErrorCode.ALREADY_OWNED_COLOR)
        if (userCharacterColorRepository.existsByUserIdAndColor(userId, color)) {
            throw DigdaException(ErrorCode.ALREADY_OWNED_COLOR)
        }
        if (character.coin < color.cost) throw DigdaException(ErrorCode.INSUFFICIENT_COIN)

        character.deductCoin(color.cost)
        userCharacterColorRepository.save(
            UserCharacterColor(
                user = character.user,
                color = color,
                pricePaid = color.cost
            )
        )
        log.info("action=character_buy_color, userId={}, color={}, cost={}, balanceAfter={}", userId, color, color.cost, character.coin)

        return getColorShop(userId)
    }

    @Transactional
    override fun applyColor(userId: UUID, color: CharacterColor): CharacterStateResponse {
        val character = loadOrCreate(userId)
        if (!color.isDefault && !userCharacterColorRepository.existsByUserIdAndColor(userId, color)) {
            throw DigdaException(ErrorCode.COLOR_NOT_OWNED)
        }
        character.applyColor(color)
        log.info("action=character_apply_color, userId={}, color={}", userId, color)
        return CharacterStateResponse.from(character)
    }

    private fun loadOrCreate(userId: UUID): UserCharacter {
        userCharacterRepository.findByUserId(userId)?.let { return it }
        val user = userRepository.findById(userId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }
        val fresh = userCharacterRepository.save(UserCharacter(user = user))
        log.info("action=character_create, userId={}, characterId={}", userId, fresh.id)
        return fresh
    }
}
