package digdaserver.admin.announcement.presentation.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

@Schema(description = "관리자 공지 발송 요청")
data class SendAnnouncementRequest(

    @field:NotBlank
    @field:Size(max = 100)
    @Schema(description = "공지 제목", example = "서비스 점검 안내")
    val title: String,

    @field:NotBlank
    @field:Size(max = 1000)
    @Schema(description = "공지 본문", example = "5월 1일 02:00 ~ 04:00 점검이 진행됩니다.")
    val body: String,

    @Schema(
        description = "발송 대상. ALL = 전체 사용자, USER_IDS = userIds로 지정한 유저",
        allowableValues = ["ALL", "USER_IDS"]
    )
    val target: String = "ALL",

    @Schema(description = "target=USER_IDS일 때 수신자 UUID 목록")
    val userIds: List<UUID>? = null
)
