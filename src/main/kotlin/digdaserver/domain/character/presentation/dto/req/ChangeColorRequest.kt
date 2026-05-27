package digdaserver.domain.character.presentation.dto.req

import digdaserver.domain.character.domain.entity.CharacterColor

data class ChangeColorRequest(
    val color: CharacterColor
)
