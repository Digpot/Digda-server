package digdaserver.domain.block.domain.repository

import digdaserver.domain.block.domain.entity.UserBlock
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserBlockRepository : JpaRepository<UserBlock, Long> {

    fun existsByBlockerIdAndBlockedId(blockerId: UUID, blockedId: UUID): Boolean

    fun findByBlockerIdAndBlockedId(blockerId: UUID, blockedId: UUID): UserBlock?

    /** 차단 목록(마이페이지) — 최신순. blocked 를 fetch 해 이름/프로필을 한 번에 로드. */
    @Query(
        "SELECT b FROM UserBlock b JOIN FETCH b.blocked WHERE b.blocker.id = :blockerId ORDER BY b.createdAt DESC"
    )
    fun findAllByBlockerIdWithBlocked(@Param("blockerId") blockerId: UUID): List<UserBlock>

    /** 조회 필터용 — 내가 차단한 사용자 ID 집합. */
    @Query("SELECT b.blocked.id FROM UserBlock b WHERE b.blocker.id = :blockerId")
    fun findBlockedIdsByBlockerId(@Param("blockerId") blockerId: UUID): List<UUID>
}
