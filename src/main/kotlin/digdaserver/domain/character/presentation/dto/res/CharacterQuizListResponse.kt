package digdaserver.domain.character.presentation.dto.res

data class CharacterQuizListResponse(
    val items: List<CharacterQuizResponse>,
    val page: Int,
    val totalPages: Int,
    val totalElements: Long
)
