package digdaserver.admin.highlight.presentation.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Pattern

@Schema(description = "월간 하이라이트 수동 생성 요청")
data class AdminGenerateHighlightRequest(

    @field:Pattern(regexp = "^[0-9]{4}-(0[1-9]|1[0-2])$")
    @Schema(description = "대상 월 yyyy-MM. 비우면 지난달(KST)", example = "2026-10")
    val month: String? = null,

    @Schema(description = "특정 그룹만 생성. 비우면 일기가 있는 전체 그룹")
    val groupRoomId: Long? = null,

    @Schema(description = "true 면 이미 있는 하이라이트도 지우고 다시 만든다(알림 없음)")
    val regenerate: Boolean = false,

    @Schema(description = "새로 만든 그룹에 알림을 보낼지(regenerate=true 면 무시)")
    val notify: Boolean = true
)
