package digdaserver.domain.title.presentation.dto.req

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 그룹 모찌에 칭호를 장착/해제한다. [code] 가 null 이면 해제.
 * 본인이 획득한 칭호만 장착 가능하며, 그룹 구성원만 호출할 수 있다.
 */
@Schema(description = "그룹 모찌 칭호 장착 요청")
data class EquipTitleRequest(
    @field:Schema(description = "그룹방 id")
    val groupRoomId: Long,

    @field:Schema(description = "장착할 칭호 코드(null = 해제)", example = "region_gyeongnam")
    val code: String? = null
)
