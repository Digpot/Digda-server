package digdaserver.domain.character.presentation.dto.req

data class SubmitAttemptRequest(
    val selectedIndex: Int,
    /** 연습(재풀이) 모드. true 면 이미 푼 문제·본인 출제도 풀 수 있고 보상(경험치/코인)은 없다. */
    val practice: Boolean = false
)
