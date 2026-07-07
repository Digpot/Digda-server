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
    val feedbackUrl: String,

    @field:Schema(description = "강제 업데이트 최소 버전(semver). 빈 값 = 게이트 끔")
    val minAppVersion: String = "",

    @field:Schema(description = "안드로이드 스토어 URL(빈 값 = 앱 기본 폴백)")
    val storeUrlAndroid: String = "",

    @field:Schema(description = "iOS App Store URL(빈 값 = 앱 기본 폴백)")
    val storeUrlIos: String = ""
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
            feedbackUrl = c.feedbackUrl,
            minAppVersion = c.minAppVersion,
            storeUrlAndroid = c.storeUrlAndroid,
            storeUrlIos = c.storeUrlIos
        )
    }
}
