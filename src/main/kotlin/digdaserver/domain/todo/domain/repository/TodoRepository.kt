package digdaserver.domain.todo.domain.repository

import digdaserver.domain.todo.domain.entity.Todo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TodoRepository : JpaRepository<Todo, Long> {

    @Query("SELECT t FROM Todo t WHERE t.groupRoom.id = :groupRoomId ORDER BY t.completed ASC, t.createdAt ASC")
    fun findAllByGroupRoomIdOrdered(groupRoomId: Long): List<Todo>

    fun countByGroupRoomId(groupRoomId: Long): Int

    fun countByGroupRoomIdAndCompletedTrue(groupRoomId: Long): Int

    @Modifying
    @Query("UPDATE Todo t SET t.completedBy = null, t.completedAt = null, t.completed = false WHERE t.completedBy.id = :userId")
    fun clearCompletedByUserId(@Param("userId") userId: UUID)

    @Modifying
    @Query("DELETE FROM Todo t WHERE t.createdBy.id = :userId")
    fun deleteAllByCreatedById(@Param("userId") userId: UUID)
}
