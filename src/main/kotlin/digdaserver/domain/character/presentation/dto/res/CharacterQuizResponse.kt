package digdaserver.domain.character.presentation.dto.res

import digdaserver.domain.character.domain.entity.CharacterQuiz
import digdaserver.domain.character.domain.entity.QuizCategory
import java.time.LocalDateTime

/**
 * 퀴즈 1건. 응시 화면에서 사용되며, 정답 인덱스는 응답에 포함하지 않는다 (응시 전 노출 방지).
 * 정답 확인은 응시 결과 응답([QuizAttemptResultResponse]) 에서만 노출.
 *
 * [createdAt] 은 클라이언트가 퀴즈 목록을 날짜별로 그룹화할 수 있도록 함께 내려준다.
 *
 * [remainingCount] 는 퀴즈 풀기 화면(pickRandom) 에서만 채워지는 "지금 풀 수 있는 남은
 * 퀴즈 수"(현재 문제 포함). 목록/생성 응답에서는 null.
 */
data class CharacterQuizResponse(
    val id: Long,
    val groupRoomId: Long,
    val category: QuizCategory,
    val categoryDisplayName: String,
    val question: String,
    val options: List<String>,
    val expMultiplier: Int,
    val authorName: String,
    val imageUrl: String?,
    val createdAt: LocalDateTime,
    val remainingCount: Int? = null
) {
    companion object {
        fun from(quiz: CharacterQuiz, remainingCount: Int? = null): CharacterQuizResponse {
            return CharacterQuizResponse(
                id = quiz.id,
                groupRoomId = quiz.groupRoom.id,
                category = quiz.category,
                categoryDisplayName = quiz.category.displayName,
                question = quiz.question,
                options = quiz.options(),
                expMultiplier = quiz.expMultiplier,
                authorName = quiz.author?.name ?: "탈퇴자",
                imageUrl = quiz.imageUrl,
                createdAt = quiz.createdAt,
                remainingCount = remainingCount
            )
        }
    }
}
