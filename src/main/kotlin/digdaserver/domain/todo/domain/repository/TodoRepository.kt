package digdaserver.domain.todo.domain.repository

import digdaserver.domain.todo.domain.entity.Todo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface TodoRepository : JpaRepository<Todo, Long> {

    @Query("SELECT t FROM Todo t WHERE t.group.id = :groupId ORDER BY t.completed ASC, t.createdAt ASC")
    fun findAllByGroupIdOrdered(groupId: Long): List<Todo>

    fun countByGroupId(groupId: Long): Int

    fun countByGroupIdAndCompletedTrue(groupId: Long): Int
}
