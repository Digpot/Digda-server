package digdaserver.domain.character.domain.repository

import digdaserver.domain.character.domain.entity.CharacterQuiz
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CharacterQuizRepository : JpaRepository<CharacterQuiz, Long> {

    @Query(
        """
        SELECT q FROM CharacterQuiz q
        WHERE q.groupRoom.id = :groupRoomId
        ORDER BY q.createdAt DESC
        """
    )
    fun findPageByGroupRoomId(
        @Param("groupRoomId") groupRoomId: Long,
        pageable: Pageable
    ): Page<CharacterQuiz>

    /**
     * 자기 작성이 아니고 + **아직 아무도 풀지 않은** 퀴즈 목록.
     * 퀴즈는 한 문제당 한 명만 풀 수 있다 — 누군가 응시한 퀴즈는 후보에서 제외한다.
     * 랜덤 픽은 서비스 계층에서 결과 리스트에 대해 Kotlin random 으로 수행
     * (JPQL/DB 종속 RAND() 회피).
     *
     * [excludeImageQuiz]=true 면 imageUrl 이 있는 (사진) 퀴즈를 후보에서 제외 —
     * 디코가 풀리지 않은 그룹에서 사진 퀴즈를 풀지 못하게 만들 때 사용한다.
     */
    @Query(
        """
        SELECT q FROM CharacterQuiz q
        WHERE q.groupRoom.id = :groupRoomId
          AND (q.author IS NULL OR q.author.id <> :userId)
          AND (:excludeImageQuiz = false OR q.imageUrl IS NULL)
          AND NOT EXISTS (
            SELECT 1 FROM CharacterQuizAttempt a
            WHERE a.quiz = q
          )
        """
    )
    fun findAvailableForUser(
        @Param("groupRoomId") groupRoomId: Long,
        @Param("userId") userId: UUID,
        @Param("excludeImageQuiz") excludeImageQuiz: Boolean
    ): List<CharacterQuiz>
}
