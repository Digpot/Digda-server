package digdaserver.domain.group_room.domain.repository

import digdaserver.domain.group_room.domain.entity.GroupRoom
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface GroupRoomRepository : JpaRepository<GroupRoom, Long> {

    @Query("SELECT g FROM GroupRoom g WHERE g.deleteScheduledAt IS NOT NULL AND g.deleteScheduledAt <= :now AND g.deletedAt IS NULL")
    fun findAllScheduledForDeletion(now: LocalDateTime): List<GroupRoom>

    fun existsByOwnerIdAndDeletedAtIsNull(ownerId: UUID): Boolean

    fun countByDeletedAtIsNull(): Long

    fun countByDeleteScheduledAtIsNotNullAndDeletedAtIsNull(): Long

    @Query(
        """
        SELECT g FROM GroupRoom g
        WHERE (:keyword IS NULL OR :keyword = ''
            OR LOWER(g.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:includeDeleted = true OR g.deletedAt IS NULL)
        """
    )
    fun searchForAdmin(
        @Param("keyword") keyword: String?,
        @Param("includeDeleted") includeDeleted: Boolean,
        pageable: Pageable
    ): Page<GroupRoom>
}
