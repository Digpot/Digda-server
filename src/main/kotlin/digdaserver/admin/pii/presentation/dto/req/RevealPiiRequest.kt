package digdaserver.admin.pii.presentation.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

enum class PiiTargetType {
    /** 사용자 — 이름·표시 이름·이메일 */
    USER,

    /** 계정/데이터 삭제 요청 — 접수 이메일 (회원이 아닐 수도 있어 USER 와 따로 둔다) */
    DELETION_REQUEST,

    /** DB 테이블 조회의 한 행 — 비밀번호·토큰은 열람해도 가려진 채로 나간다 */
    DB_ROW
}

@Schema(description = "개인정보 원문 열람 요청 — 관리자 비밀번호 재확인")
data class RevealPiiRequest(

    @field:NotBlank
    @field:Size(max = 100)
    @Schema(description = "현재 로그인한 관리자의 비밀번호")
    val password: String,

    @field:NotNull
    @Schema(description = "열람 대상 종류")
    val targetType: PiiTargetType,

    @field:Size(max = 64)
    @Schema(description = "USER=userId(UUID), DELETION_REQUEST=요청 ID. DB_ROW 는 비움")
    val targetId: String? = null,

    @field:Size(max = 64)
    @Schema(description = "DB_ROW 전용 — 테이블명")
    val table: String? = null,

    @Schema(description = "DB_ROW 전용 — PK 컬럼명 → 값")
    val pk: Map<String, String>? = null
)
