package digdaserver.domain.character.domain.repository

import digdaserver.domain.character.domain.entity.CharacterQuizAttempt
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CharacterQuizAttemptRepository : JpaRepository<CharacterQuizAttempt, Long> {
    fun existsByQuizIdAndUserId(quizId: Long, userId: UUID): Boolean

    /** 퀴즈당 한 명만 풀 수 있다 — 누군가(본인 포함) 이미 응시했는지. */
    fun existsByQuizId(quizId: Long): Boolean

    /** 특정 유저가 주어진 퀴즈들에 남긴 응시 기록(목록 화면의 정답/오답 표시용). */
    fun findAllByUserIdAndQuizIdIn(userId: UUID, quizIds: List<Long>): List<CharacterQuizAttempt>

    /**
     * 주어진 퀴즈들의 모든 응시 기록 — 목록에서 "누가 풀었고 맞았는지" 표시용.
     * user 를 fetch join 해 닉네임 접근 시 N+1 을 피한다. 응시순 정렬.
     */
    @Query(
        "SELECT a FROM CharacterQuizAttempt a JOIN FETCH a.user " +
            "WHERE a.quiz.id IN :quizIds ORDER BY a.attemptedAt ASC"
    )
    fun findAllWithUserByQuizIdIn(@Param("quizIds") quizIds: List<Long>): List<CharacterQuizAttempt>
}
