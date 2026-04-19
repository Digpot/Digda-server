package digdaserver.domain.todo.application.service

import digdaserver.domain.todo.presentation.dto.res.TodoListResponse
import digdaserver.domain.todo.presentation.dto.res.TodoResponse
import java.util.UUID

interface TodoService {

    fun getTodos(userId: UUID, groupRoomId: Long): TodoListResponse

    fun createTodo(userId: UUID, groupRoomId: Long, text: String): TodoResponse

    fun toggleTodo(userId: UUID, groupRoomId: Long, todoId: Long, completed: Boolean): TodoResponse

    fun deleteTodo(userId: UUID, groupRoomId: Long, todoId: Long)
}
