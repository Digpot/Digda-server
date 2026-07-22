package digdaserver.domain.appconfig.presentation.dto.req

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "앱 전역 운영 설정 수정")
data class UpdateAppConfigRequest(
    val noticeEnabled: Boolean = false,
    val noticeMessage: String = "",
    val feedbackEnabled: Boolean = false,
    val feedbackUrl: String = "",
    // 강제 업데이트 게이트 — null 이면 기존 값 유지(구버전 어드민 호환).
    val minAppVersion: String? = null,
    val storeUrlAndroid: String? = null,
    val storeUrlIos: String? = null,
    // 서버 점검 모드 — null 이면 기존 값 유지(구버전 어드민 호환).
    val maintenanceEnabled: Boolean? = null,
    val maintenanceMessage: String? = null
)
