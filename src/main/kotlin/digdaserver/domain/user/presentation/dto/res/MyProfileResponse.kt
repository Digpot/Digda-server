package digdaserver.domain.user.presentation.dto.res

import digdaserver.domain.user.domain.entity.User
import java.time.LocalDateTime

data class MyProfileResponse(
    val id: String,
    val name: String,
    val email: String?,
    val profileImage: String?,
    val provider: String,
    val createdAt: LocalDateTime,
    // 서비스 이용 제한 여부 — true 면 앱이 마이페이지 외 기능을 막는다.
    val restricted: Boolean
) {
    companion object {
        fun from(user: User): MyProfileResponse = MyProfileResponse(
            id = user.id.toString(),
            name = user.displayedName(),
            email = user.email,
            profileImage = user.profileImage,
            provider = user.socialProvider.name.lowercase(),
            createdAt = user.createdAt,
            restricted = user.restricted
        )
    }
}
