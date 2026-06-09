package digdaserver.domain.appconfig.presentation.dto.res

import digdaserver.domain.appconfig.domain.entity.AppConfig
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "앱 전역 운영 설정")
data class AppConfigResponse(
    @field:Schema(description = "대공지 노출 여부")
    val noticeEnabled: Boolean,

    @field:Schema(description = "대공지 메시지")
    val noticeMessage: String,

    @field:Schema(description = "피드백 메뉴 노출 여부")
    val feedbackEnabled: Boolean,

    @field:Schema(description = "피드백 폼 URL")
    val feedbackUrl: String
) {
    companion object {
        val default = AppConfigResponse(
            noticeEnabled = false,
            noticeMessage = "",
            feedbackEnabled = false,
            feedbackUrl = ""
        )

        fun from(c: AppConfig): AppConfigResponse = AppConfigResponse(
            noticeEnabled = c.noticeEnabled,
            noticeMessage = c.noticeMessage,
            feedbackEnabled = c.feedbackEnabled,
            feedbackUrl = c.feedbackUrl
        )
    }
}
