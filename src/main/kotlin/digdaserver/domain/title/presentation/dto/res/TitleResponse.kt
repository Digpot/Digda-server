package digdaserver.domain.title.presentation.dto.res

import digdaserver.domain.title.domain.entity.UserTitle
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * 획득한 칭호 1건. 표시 메타(이름/아이콘/색)는 앱 카탈로그가 [code] 로 매핑한다.
 */
@Schema(description = "획득 칭호")
data class TitleResponse(
    @field:Schema(description = "칭호 코드", example = "region_gyeongnam")
    val code: String,

    @field:Schema(description = "획득 그룹방 id(전역 칭호는 null)")
    val groupRoomId: Long?,

    @field:Schema(description = "획득 당시 그룹방 이름 스냅샷(전역 칭호는 null)")
    val groupRoomName: String?,

    @field:Schema(description = "획득 시각")
    val earnedAt: LocalDateTime
) {
    companion object {
        fun from(e: UserTitle): TitleResponse = TitleResponse(
            code = e.code,
            groupRoomId = e.groupRoomId,
            groupRoomName = e.groupRoomName,
            earnedAt = e.earnedAt
        )
    }
}
