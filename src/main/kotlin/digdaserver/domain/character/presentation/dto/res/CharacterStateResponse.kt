package digdaserver.domain.character.presentation.dto.res

import digdaserver.domain.character.application.level.CharacterLevelTable
import digdaserver.domain.character.domain.entity.CharacterColor
import digdaserver.domain.character.domain.entity.CharacterStage
import digdaserver.domain.character.domain.entity.GroupCharacter

/**
 * 캐릭터 현재 상태 응답. 클라가 progress bar 등을 계산할 수 있도록 next-level 임계치까지
 * 함께 내려준다 (서버가 곡선의 진실 출처).
 */
data class CharacterStateResponse(
    val stage: CharacterStage,
    val stageDisplayName: String,
    val color: CharacterColor,
    val colorHex: String,
    val level: Int,
    val exp: Int,
    val expForNextLevel: Int,
    val coin: Int,
    val maxLevelReached: Boolean
) {
    companion object {
        fun from(character: GroupCharacter): CharacterStateResponse {
            val nextThreshold = CharacterLevelTable.expForNextLevel(character.level)
            val atMax = character.level >= CharacterLevelTable.MAX_LEVEL
            return CharacterStateResponse(
                stage = character.stage,
                stageDisplayName = character.stage.displayName,
                color = character.color,
                colorHex = character.color.hex,
                level = character.level,
                exp = character.exp,
                expForNextLevel = if (atMax) 0 else nextThreshold,
                coin = character.coin,
                maxLevelReached = atMax
            )
        }
    }
}
