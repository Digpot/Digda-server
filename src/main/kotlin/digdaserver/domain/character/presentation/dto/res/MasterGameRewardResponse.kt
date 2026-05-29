package digdaserver.domain.character.presentation.dto.res

/**
 * 마스터 모찌 챔피언 챌린지 보상 응답.
 *
 * - [score]: 클라가 보낸 점수 (서버는 음수만 0 으로 클램프)
 * - [coinReward]: 점수 등급에 따라 그룹 캐릭터에 가산된 코인량
 * - [tier]: 등급 라벨 (도전/우수/훌륭/전설)
 * - [character]: 코인이 반영된 갱신 캐릭터 상태
 */
data class MasterGameRewardResponse(
    val score: Int,
    val coinReward: Int,
    val tier: String,
    val character: CharacterStateResponse
)
