package digdaserver.domain.todo.presentation.dto.res

data class ProgressResponse(
    val total: Int,
    val completed: Int,
    val percent: Int
)
