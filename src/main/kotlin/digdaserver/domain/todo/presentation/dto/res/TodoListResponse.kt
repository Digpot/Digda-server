package digdaserver.domain.todo.presentation.dto.res

data class TodoListResponse(
    val todos: List<TodoResponse>,
    val progress: ProgressResponse
)
