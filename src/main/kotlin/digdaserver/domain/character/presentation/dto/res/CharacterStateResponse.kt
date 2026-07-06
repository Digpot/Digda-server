package digdaserver.domain.character.presentation.dto.res

import digdaserver.domain.character.application.level.CharacterLevelTable
import digdaserver.domain.character.domain.entity.CharacterStage
import digdaserver.domain.character.domain.entity.GroupCharacter
import digdaserver.domain.character.domain.entity.GroupCharacterEquipped

/**
 * 캐릭터 현재 상태 응답. 클라가 progress bar 등을 계산할 수 있도록 next-level 임계치까지
 * 함께 내려준다 (서버가 곡선의 진실 출처).
 *
 * 외형은 [equippedItems] 만으로 표현 — 클라는 layer_order 오름차순으로 그리며,
 * SKIN 슬롯의 [EquippedItemResponse.accentColor] 가 배경 squircle 색이 된다.
 */
data class CharacterStateResponse(
    val stage: CharacterStage,
    val stageDisplayName: String,
    val level: Int,
    val exp: Int,
    val expForNextLevel: Int,
    val coin: Int,
    val maxLevelReached: Boolean,
    val dikoUnlocked: Boolean,
    /** 마스터 진화 시험(챔피언 챌린지) 통과 여부. true 면 stage 가 MASTER. */
    val masterUnlocked: Boolean,
    /** 레벨 20 도달 — 챔피언 챌린지(진화 시험/코인 파밍) 응시 가능 여부. */
    val canChallengeMaster: Boolean,
    val equippedItems: List<EquippedItemResponse>
) {
    companion object {
        fun from(
            character: GroupCharacter,
            equipped: List<GroupCharacterEquipped> = emptyList()
        ): CharacterStateResponse {
            val nextThreshold = CharacterLevelTable.expForNextLevel(character.level)
            val atMax = character.level >= CharacterLevelTable.MAX_LEVEL
            return CharacterStateResponse(
                stage = character.stage,
                stageDisplayName = character.stage.displayName,
                level = character.level,
                exp = character.exp,
                expForNextLevel = if (atMax) 0 else nextThreshold,
                coin = character.coin,
                maxLevelReached = atMax,
                dikoUnlocked = character.dikoUnlocked,
                masterUnlocked = character.masterUnlocked,
                canChallengeMaster = atMax,
                equippedItems = equipped
                    .sortedBy { it.shopItem.layerOrder }
                    .map(EquippedItemResponse::from)
            )
        }
    }
}
