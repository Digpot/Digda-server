package digdaserver.admin.nicknameexhibit.presentation.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

/**
 * 전시관 별명 카드 부분 수정. 모든 필드 선택 — null 이면 변경하지 않음.
 */
@Schema(description = "전시관 별명 카드 수정 요청")
data class UpdateNicknameExhibitRequest(

    @field:Size(max = 100)
    @Schema(description = "변경할 별명. 미전송 시 변경 없음.")
    val nickname: String? = null,

    @field:Size(max = 512)
    @Schema(description = "변경할 이미지 URL. 미전송 시 변경 없음.")
    val imageUrl: String? = null,

    @Schema(description = "변경할 별명 역사/설명. 미전송 시 변경 없음.")
    val history: String? = null,

    @Schema(description = "변경할 정렬 순서. 미전송 시 변경 없음.")
    val sortOrder: Int? = null
)
