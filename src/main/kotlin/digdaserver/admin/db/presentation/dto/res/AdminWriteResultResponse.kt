package digdaserver.admin.db.presentation.dto.res

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "테이블 쓰기 작업 결과")
data class AdminWriteResultResponse(

    @Schema(description = "영향받은 행 수")
    val affected: Int
)
