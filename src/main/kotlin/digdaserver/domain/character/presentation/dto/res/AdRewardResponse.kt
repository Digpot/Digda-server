package digdaserver.domain.character.presentation.dto.res

/**
 * 광고 시청 보상 응답.
 *
 * - [coinReward]: 이번에 적립된 코인량(서버가 결정 — 클라 조작 방지)
 * - [dailyRemaining]: 오늘 더 받을 수 있는 광고 보상 횟수
 * - [character]: 코인이 반영된 갱신 캐릭터 상태
 */
data class AdRewardResponse(
    val coinReward: Int,
    val dailyRemaining: Int,
    val character: CharacterStateResponse
)
