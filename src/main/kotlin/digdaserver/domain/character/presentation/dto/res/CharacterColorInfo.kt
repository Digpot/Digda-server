package digdaserver.domain.character.presentation.dto.res

import digdaserver.domain.character.domain.entity.CharacterColor

data class CharacterColorInfo(
    val color: CharacterColor,
    val displayName: String,
    val hex: String,
    val cost: Int,
    val owned: Boolean,
    val isCurrent: Boolean,
    val isDefault: Boolean
)

data class CharacterColorShopResponse(
    val coin: Int,
    val items: List<CharacterColorInfo>
)
