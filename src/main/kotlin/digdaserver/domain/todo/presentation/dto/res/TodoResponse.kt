package digdaserver.domain.todo.presentation.dto.res

import digdaserver.domain.todo.domain.entity.Todo
import java.time.LocalDateTime

data class TodoResponse(
    val id: Long,
    val text: String,
    val completed: Boolean,
    val completedAt: LocalDateTime?,
    val completedBy: TodoUserSummary?,
    val createdBy: TodoUserSummary,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(todo: Todo): TodoResponse = TodoResponse(
            id = todo.id,
            text = todo.text,
            completed = todo.completed,
            completedAt = todo.completedAt,
            completedBy = todo.completedBy?.let { TodoUserSummary.from(it) },
            createdBy = TodoUserSummary.from(todo.createdBy),
            createdAt = todo.createdAt
        )
    }
}
