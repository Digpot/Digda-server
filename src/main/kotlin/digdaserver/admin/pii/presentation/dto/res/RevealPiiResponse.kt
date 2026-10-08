package digdaserver.admin.pii.presentation.dto.res

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "개인정보 원문 — 필드명 → 값")
data class RevealPiiResponse(
    val fields: Map<String, Any?>
)
