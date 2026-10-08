package digdaserver.admin.event.presentation.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

@Schema(description = "코인 전체 지급 요청")
data class GrantCoinRequest(

    @field:Min(1)
    @field:Max(100_000)
    @Schema(description = "그룹(모찌) 1개당 지급할 코인", example = "500")
    val amount: Int,

    @field:Size(max = 200)
    @Schema(description = "운영 메모(이력에 남는다)", example = "2026 추석 이벤트")
    val reason: String = "",

    @Schema(description = "지급과 함께 전체 공지 푸시를 보낼지")
    val notify: Boolean = false,

    @field:Size(max = 100)
    @Schema(description = "notify=true 일 때 공지 제목", example = "추석 선물 도착! 🌕")
    val notificationTitle: String? = null,

    @field:Size(max = 1000)
    @Schema(description = "notify=true 일 때 공지 본문", example = "모찌에게 코인 500개를 선물했어요. 지금 확인해 보세요!")
    val notificationBody: String? = null
)
