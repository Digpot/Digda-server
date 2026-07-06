package digdaserver.admin.nicknameexhibit.presentation.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "전시관 접근 허용 추가 요청")
data class AddExhibitAccessRequest(

    @Schema(description = "허용할 사용자 ID")
    val userId: UUID
)
