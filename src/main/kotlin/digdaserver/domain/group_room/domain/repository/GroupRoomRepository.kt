package digdaserver.domain.group_room.domain.repository

import digdaserver.domain.group_room.domain.entity.GroupRoom
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface GroupRoomRepository : JpaRepository<GroupRoom, Long> {

    @Query("SELECT g FROM GroupRoom g WHERE g.deleteScheduledAt IS NOT NULL AND g.deleteScheduledAt <= :now AND g.deletedAt IS NULL")
    fun findAllScheduledForDeletion(now: LocalDateTime): List<GroupRoom>

    fun existsByOwnerIdAndDeletedAtIsNull(ownerId: UUID): Boolean
}
