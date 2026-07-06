package digdaserver.domain.oauth2.presentation.dto.res

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "약관 문서 응답")
data class TermsDocumentResponse(

    @Schema(description = "약관 제목", example = "이용약관")
    val title: String,

    @Schema(description = "약관 본문 (HTML)")
    val content: String,

    @Schema(description = "약관 버전", example = "v1.0")
    val version: String,

    @Schema(description = "최종 수정일", example = "2026-03-01")
    val updatedAt: String
)
