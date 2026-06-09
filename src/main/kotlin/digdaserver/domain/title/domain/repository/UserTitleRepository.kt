package digdaserver.domain.title.domain.repository

import digdaserver.domain.title.domain.entity.UserTitle
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserTitleRepository : JpaRepository<UserTitle, Long> {

    /** 한 사용자가 획득한 모든 칭호 — 최신 획득순. */
    fun findAllByUserIdOrderByEarnedAtDesc(userId: UUID): List<UserTitle>

    fun existsByUserIdAndCode(userId: UUID, code: String): Boolean
}
