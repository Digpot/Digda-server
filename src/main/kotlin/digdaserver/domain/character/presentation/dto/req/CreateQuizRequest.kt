package digdaserver.domain.character.presentation.dto.req

import digdaserver.domain.character.domain.entity.QuizCategory

/**
 * 퀴즈 생성 요청.
 *
 * [imageUrl] 은 선택 — 채워서 보내면 이미지 퀴즈, null/빈 문자열이면 기존 텍스트 퀴즈.
 * 업로드는 별도로 `/uploads/images?purpose=quiz` 를 호출해 URL 을 받은 뒤 여기에 넣는다.
 */
data class CreateQuizRequest(
    val groupRoomId: Long,
    val category: QuizCategory,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val expMultiplier: Int,
    val imageUrl: String? = null
)
