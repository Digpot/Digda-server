package digdaserver.domain.oauth2.presentation.dto.req

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "약관 동의 요청")
data class TermsAgreeRequest(

    @Schema(description = "이용약관 동의 (필수)", example = "true")
    val termsOfService: Boolean,

    @Schema(description = "개인정보처리방침 동의 (필수)", example = "true")
    val privacyPolicy: Boolean,

    @Schema(description = "만 14세 이상 확인 (필수)", example = "true")
    val ageConfirmation: Boolean,

    @Schema(description = "마케팅 수신 동의 (선택)", example = "false")
    val marketingConsent: Boolean = false,

    @Schema(description = "푸시 알림 수신 동의 (선택)", example = "false")
    val pushConsent: Boolean = false
)
