package digdaserver.admin.db.presentation.dto.res

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "DB 컬럼 정보")
data class AdminColumnInfoResponse(

    @Schema(description = "컬럼명")
    val columnName: String,

    @Schema(description = "데이터 타입(예: varchar, bigint)")
    val dataType: String,

    @Schema(description = "컬럼 전체 타입(예: varchar(255))")
    val columnType: String,

    @Schema(description = "NULL 허용 여부")
    val nullable: Boolean,

    @Schema(description = "기본값")
    val defaultValue: String?,

    @Schema(description = "키(PRI/UNI/MUL)")
    val columnKey: String?,

    @Schema(description = "주석")
    val comment: String?,

    @Schema(description = "정렬 순서")
    val ordinalPosition: Int
)
