package digdaserver.domain.character.presentation.dto.res

import digdaserver.domain.character.domain.entity.CharacterQuizAttempt
import java.time.LocalDateTime
import java.util.UUID

/**
 * 퀴즈 목록에서 "누가 풀었고 맞았는지"를 보여주기 위한 응시 요약 1건.
 * [userName] 은 프로필에서 지정한 닉네임 우선([User.displayedName]) — 탈퇴자는 안 내려간다
 * (attempt 는 user FK 필수라 탈퇴 시 함께 정리됨).
 */
data class QuizAttemptSummary(
    val userId: UUID,
    val userName: String,
    val correct: Boolean,
    val attemptedAt: LocalDateTime
) {
    companion object {
        fun from(attempt: CharacterQuizAttempt): QuizAttemptSummary = QuizAttemptSummary(
            userId = attempt.user.id,
            userName = attempt.user.displayedName(),
            correct = attempt.correct,
            attemptedAt = attempt.attemptedAt
        )
    }
}
