package digdaserver.domain.character.presentation.dto.res

import digdaserver.domain.character.domain.entity.CharacterStage
import digdaserver.domain.character.domain.entity.GroupCharacter
import digdaserver.domain.character.domain.entity.GroupCharacterEquipped

/**
 * exp 가산 결과 + 갱신된 캐릭터 상태. 클라이언트는 [levelGained]>0 이면 레벨업 연출,
 * [stageChanged]=true 이면 진화 연출을 띄울 수 있다.
 */
data class AddExpResponse(
    val character: CharacterStateResponse,
    val levelGained: Int,
    val stageBefore: CharacterStage,
    val stageAfter: CharacterStage,
    val stageChanged: Boolean,
    val coinDelta: Int
) {
    companion object {
        fun from(
            character: GroupCharacter,
            result: GroupCharacter.GainResult,
            coinDelta: Int,
            equipped: List<GroupCharacterEquipped> = emptyList()
        ): AddExpResponse {
            return AddExpResponse(
                character = CharacterStateResponse.from(character, equipped),
                levelGained = result.levelGained,
                stageBefore = result.stageBefore,
                stageAfter = result.stageAfter,
                stageChanged = result.stageChanged,
                coinDelta = coinDelta
            )
        }
    }
}
