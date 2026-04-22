package digdaserver.admin.db.presentation.dto.res

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "DB 테이블 데이터 조회 응답 (컬럼 기반)")
data class AdminTableRowsResponse(

    @Schema(description = "테이블명")
    val tableName: String,

    @Schema(description = "컬럼 순서(응답 rows 의 키 순서)")
    val columns: List<String>,

    @Schema(description = "현재 페이지(0-base)")
    val page: Int,

    @Schema(description = "페이지 크기")
    val size: Int,

    @Schema(description = "총 행 수")
    val totalElements: Long,

    @Schema(description = "총 페이지 수")
    val totalPages: Int,

    @Schema(description = "행 데이터 목록. 각 행은 컬럼명 → 값")
    val rows: List<Map<String, Any?>>
)
