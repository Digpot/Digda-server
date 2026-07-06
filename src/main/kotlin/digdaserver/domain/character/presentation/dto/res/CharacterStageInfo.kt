package digdaserver.domain.character.presentation.dto.res

import digdaserver.domain.character.domain.entity.CharacterStage

/** 진화 트리 카드 1개. 클라가 잠금/도달 표시를 만들 수 있도록 unlocked 도 함께 내려준다. */
data class CharacterStageInfo(
    val stage: CharacterStage,
    val displayName: String,
    val requiredLevel: Int,
    val unlocked: Boolean
)

data class CharacterStageTreeResponse(
    val currentStage: CharacterStage,
    val currentLevel: Int,
    val stages: List<CharacterStageInfo>
)
