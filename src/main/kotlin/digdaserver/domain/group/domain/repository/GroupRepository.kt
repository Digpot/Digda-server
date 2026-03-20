package digdaserver.domain.group.domain.repository

import digdaserver.domain.group.domain.entity.Group
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface GroupRepository : JpaRepository<Group, Long> {

    @Query("SELECT g FROM Group g WHERE g.deleteScheduledAt IS NOT NULL AND g.deleteScheduledAt <= :now AND g.deletedAt IS NULL")
    fun findAllScheduledForDeletion(now: LocalDateTime): List<Group>

    fun existsByOwnerIdAndDeletedAtIsNull(ownerId: Long): Boolean
}
