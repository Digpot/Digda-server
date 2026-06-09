package digdaserver.domain.appconfig.presentation.dto.req

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "앱 전역 운영 설정 수정")
data class UpdateAppConfigRequest(
    val noticeEnabled: Boolean = false,
    val noticeMessage: String = "",
    val feedbackEnabled: Boolean = false,
    val feedbackUrl: String = ""
)
