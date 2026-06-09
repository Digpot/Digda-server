package digdaserver.admin.title.presentation.dto.res

import digdaserver.domain.title.domain.entity.UserTitle
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/** 어드민용 — 사용자가 보유한 칭호 1건(카탈로그 표시명 결합). */
@Schema(description = "사용자 보유 칭호")
data class AdminUserTitleResponse(
    val code: String,
    val name: String,
    val category: String,
    val accentColor: String,
    val groupRoomName: String?,
    val earnedAt: LocalDateTime
) {
    companion object {
        fun of(
            t: UserTitle,
            name: String,
            category: String,
            accentColor: String
        ): AdminUserTitleResponse = AdminUserTitleResponse(
            code = t.code,
            name = name,
            category = category,
            accentColor = accentColor,
            groupRoomName = t.groupRoomName,
            earnedAt = t.earnedAt
        )
    }
}
