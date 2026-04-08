package digdaserver.domain.todo.domain.repository

import digdaserver.domain.todo.domain.entity.Todo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface TodoRepository : JpaRepository<Todo, Long> {

    @Query("SELECT t FROM Todo t WHERE t.groupRoom.id = :groupRoomId ORDER BY t.completed ASC, t.createdAt ASC")
    fun findAllByGroupRoomIdOrdered(groupRoomId: Long): List<Todo>

    fun countByGroupRoomId(groupRoomId: Long): Int

    fun countByGroupRoomIdAndCompletedTrue(groupRoomId: Long): Int
}
