package digdaserver.admin.region.presentation.dto.req

import io.swagger.v3.oas.annotations.media.Schema

/** 어드민이 그룹의 지역을 임의로 채우거나 해제한다. */
@Schema(description = "지역 채움/해제 요청")
data class RegionFillRequest(
    @field:Schema(description = "대상 그룹방 id")
    val groupRoomId: Long,

    @field:Schema(description = "채울/해제할 시·군·구 region_key 목록")
    val regionKeys: List<String> = emptyList()
)
