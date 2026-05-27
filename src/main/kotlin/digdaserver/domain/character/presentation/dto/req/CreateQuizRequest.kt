package digdaserver.domain.character.presentation.dto.req

import digdaserver.domain.character.domain.entity.QuizCategory

data class CreateQuizRequest(
    val groupRoomId: Long,
    val category: QuizCategory,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val expMultiplier: Int
)
