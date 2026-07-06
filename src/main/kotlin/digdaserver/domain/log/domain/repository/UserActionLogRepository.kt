package digdaserver.domain.log.domain.repository

import digdaserver.domain.log.domain.entity.UserAction
import digdaserver.domain.log.domain.entity.UserActionLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface UserActionLogRepository : JpaRepository<UserActionLog, Long> {

    @Query(
        """
        SELECT l FROM UserActionLog l
        WHERE (:actorId IS NULL OR l.actorId = :actorId)
          AND (:action IS NULL OR l.action = :action)
          AND (:from IS NULL OR l.createdAt >= :from)
          AND (:to IS NULL OR l.createdAt <= :to)
          AND (:keyword IS NULL OR :keyword = ''
               OR LOWER(COALESCE(l.detail, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(l.targetId, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """
    )
    fun searchLogs(
        @Param("actorId") actorId: UUID?,
        @Param("action") action: UserAction?,
        @Param("from") from: LocalDateTime?,
        @Param("to") to: LocalDateTime?,
        @Param("keyword") keyword: String?,
        pageable: Pageable
    ): Page<UserActionLog>
}
