package digdaserver.domain.title.presentation.dto.res

import digdaserver.domain.title.domain.entity.GroupEquippedTitle
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

/**
 * 그룹 모찌에 장착된 칭호. [code] 가 null 이면 미장착.
 * 표시 메타(이름/아이콘/색)는 앱 카탈로그가 code 로 매핑한다.
 */
@Schema(description = "그룹 모찌 장착 칭호")
data class EquippedTitleResponse(
    @field:Schema(description = "장착 칭호 코드(미장착이면 null)")
    val code: String?,

    @field:Schema(description = "장착한 사용자 id(미장착이면 null)")
    val equippedBy: UUID?
) {
    companion object {
        val empty = EquippedTitleResponse(code = null, equippedBy = null)

        fun from(e: GroupEquippedTitle): EquippedTitleResponse =
            EquippedTitleResponse(code = e.code, equippedBy = e.equippedBy)
    }
}
