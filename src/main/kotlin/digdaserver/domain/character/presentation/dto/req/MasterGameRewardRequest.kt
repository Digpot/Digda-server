package digdaserver.domain.character.presentation.dto.req

/** POST /character/master-game-reward 요청 바디. score 는 0 이상의 정수. */
data class MasterGameRewardRequest(
    val score: Int
)
