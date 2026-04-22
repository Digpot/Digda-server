package digdaserver.admin.db.presentation.dto.res

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "DB 테이블 정보")
data class AdminTableInfoResponse(

    @Schema(description = "테이블명")
    val tableName: String,

    @Schema(description = "주석")
    val tableComment: String?,

    @Schema(description = "예상 행 개수(통계, 근사값)")
    val approxRowCount: Long?
)
