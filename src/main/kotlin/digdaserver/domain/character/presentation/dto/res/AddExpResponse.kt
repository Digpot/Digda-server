package digdaserver.domain.character.presentation.dto.res

import digdaserver.domain.character.domain.entity.CharacterStage
import digdaserver.domain.character.domain.entity.GroupCharacter
import digdaserver.domain.character.domain.entity.GroupCharacterEquipped

/**
 * exp 가산 결과 + 갱신된 캐릭터 상태. 클라이언트는 [levelGained]>0 이면 레벨업 연출,
 * [stageChanged]=true 이면 진화 연출을 띄울 수 있다.
 *
 * [dikoJustUnlocked]=true 이면 디코 등장 컷씬을 1회 띄운다 (서버는 보존된 상태로 더 이상
 * true 를 내려주지 않음 — `character.dikoUnlocked` 만 true 로 유지).
 *
 * 시즌 이벤트(경험치 N배)가 열려 있으면 [expMultiplier]>1.0 이고 [bonusExp] 에 이벤트로 더
 * 받은 양이 담긴다. 이벤트가 없으면 각각 1.0 / 0 이라 구버전 앱은 무시해도 동작이 같다.
 */
data class AddExpResponse(
    val character: CharacterStateResponse,
    val levelGained: Int,
    val stageBefore: CharacterStage,
    val stageAfter: CharacterStage,
    val stageChanged: Boolean,
    val coinDelta: Int,
    val dikoJustUnlocked: Boolean,
    val expMultiplier: Double = 1.0,
    val bonusExp: Int = 0
) {
    companion object {
        fun from(
            character: GroupCharacter,
            result: GroupCharacter.GainResult,
            coinDelta: Int,
            equipped: List<GroupCharacterEquipped> = emptyList(),
            expMultiplier: Double = 1.0,
            bonusExp: Int = 0
        ): AddExpResponse {
            return AddExpResponse(
                character = CharacterStateResponse.from(character, equipped),
                levelGained = result.levelGained,
                stageBefore = result.stageBefore,
                stageAfter = result.stageAfter,
                stageChanged = result.stageChanged,
                coinDelta = coinDelta,
                dikoJustUnlocked = result.dikoJustUnlocked,
                expMultiplier = expMultiplier,
                bonusExp = bonusExp
            )
        }
    }
}
