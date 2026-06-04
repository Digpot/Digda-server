package digdaserver.domain.nickname_exhibit.presentation.dto.res

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "전시관 접근 권한 여부")
data class NicknameExhibitAccessResponse(

    @Schema(description = "전시관 접근 허용 여부")
    val allowed: Boolean
)
