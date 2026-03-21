package digdaserver.domain.oauth2.presentation.dto.res

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "약관 동의 응답")
data class TermsAgreeResponse(

    @Schema(description = "약관 동의 완료된 사용자 정보")
    val user: UserResponse
)
