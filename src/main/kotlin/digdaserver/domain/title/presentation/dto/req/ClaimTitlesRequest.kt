package digdaserver.domain.title.presentation.dto.req

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 앱이 판정한 "방금 획득한 칭호" 들을 서버에 적재 요청한다(멱등).
 * 이미 보유한 코드는 무시되고, 멤버가 아닌 그룹/형식이 잘못된 코드도 조용히 건너뛴다.
 */
@Schema(description = "칭호 획득 적재 요청")
data class ClaimTitlesRequest(
    @field:Schema(description = "획득 칭호 목록")
    val titles: List<ClaimTitleItem> = emptyList()
)

@Schema(description = "획득 칭호 1건")
data class ClaimTitleItem(
    @field:Schema(description = "앱 카탈로그 칭호 코드", example = "region_gyeongnam")
    val code: String,

    @field:Schema(description = "획득 그룹방 id(전역 칭호는 null)", example = "12")
    val groupRoomId: Long? = null
)
