package digdaserver.admin.db.presentation.dto.req

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "테이블 행 생성/수정 요청")
data class AdminUpsertRowRequest(

    @Schema(
        description = "컬럼명 → 값 (값은 문자열로 전달, 서버가 컬럼 타입에 맞게 변환). null 컬럼은 \"NULL\" 대신 JSON null 사용",
        example = """{"name":"홍길동","age":"20"}"""
    )
    val values: Map<String, String?>
)
