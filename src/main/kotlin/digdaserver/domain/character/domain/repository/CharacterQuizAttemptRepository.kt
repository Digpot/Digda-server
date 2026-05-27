package digdaserver.domain.character.domain.repository

import digdaserver.domain.character.domain.entity.CharacterQuizAttempt
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CharacterQuizAttemptRepository : JpaRepository<CharacterQuizAttempt, Long> {
    fun existsByQuizIdAndUserId(quizId: Long, userId: UUID): Boolean
}
