package digdaserver.domain.oauth2.presentation.dto.req

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "약관 동의 요청")
data class TermsAgreeRequest(

    @Schema(description = "이용약관 동의 (필수)", example = "true")
    val termsOfService: Boolean,

    @Schema(description = "개인정보처리방침 동의 (필수)", example = "true")
    val privacyPolicy: Boolean,

    @Schema(description = "연령 확인 (레거시 호환용, 미전송 시 true)", example = "true")
    val ageConfirmation: Boolean = true,

    @Schema(description = "마케팅 수신 동의 (선택)", example = "false")
    val marketingConsent: Boolean = false,

    @Schema(description = "푸시 알림 수신 동의 (선택)", example = "false")
    val pushConsent: Boolean = false
)
