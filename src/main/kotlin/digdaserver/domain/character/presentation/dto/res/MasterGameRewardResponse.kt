package digdaserver.domain.character.presentation.dto.res

/**
 * 마스터 모찌 챔피언 챌린지 보상 응답.
 *
 * - [score]: 클라가 보낸 점수 (서버는 음수만 0 으로 클램프)
 * - [coinReward]: 점수 등급에 따라 그룹 캐릭터에 가산된 코인량
 * - [tier]: 등급 라벨 (도전/우수/훌륭/전설)
 * - [evolvedToMaster]: 이번 도전(훌륭 이상)으로 마스터 진화가 일어났는지. 클라 진화 연출용.
 * - [character]: 코인/진화가 반영된 갱신 캐릭터 상태
 */
data class MasterGameRewardResponse(
    val score: Int,
    val coinReward: Int,
    val tier: String,
    val evolvedToMaster: Boolean,
    val character: CharacterStateResponse
)
