package digdaserver.domain.character.presentation.dto.req

/**
 * exp 가산 요청. [source] 는 감사/통계 목적으로 남기는 자유 문자열
 * (예: "quiz_correct", "diary_write", "manual_pat") — 서버 enum 으로 강제하지 않아
 * 클라이언트가 새 이벤트를 추가할 때마다 서버 배포가 필요하지 않게 한다.
 */
data class AddExpRequest(
    val amount: Int,
    val source: String? = null
)
