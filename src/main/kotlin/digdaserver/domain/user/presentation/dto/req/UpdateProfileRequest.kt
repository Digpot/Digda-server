package digdaserver.domain.user.presentation.dto.req

import java.util.Optional

data class UpdateProfileRequest(
    val name: String? = null,
    val statusMessage: String? = null,
    val profileImageId: Optional<String>? = null
)
