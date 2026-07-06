package digdaserver.domain.block.presentation.dto.res

import digdaserver.domain.block.domain.entity.UserBlock
import java.time.LocalDateTime
import java.util.UUID

/** 마이페이지 차단 목록 한 줄. */
data class BlockedUserResponse(
    val userId: UUID,
    val name: String,
    val profileImage: String?,
    val blockedAt: LocalDateTime
) {
    companion object {
        fun from(block: UserBlock): BlockedUserResponse = BlockedUserResponse(
            userId = block.blocked.id,
            name = block.blocked.displayedName(),
            profileImage = block.blocked.profileImage,
            blockedAt = block.createdAt
        )
    }
}
