package digdaserver.admin.character.presentation.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

/**
 * 어드민 모찌 정보 수정. 모든 필드는 선택 — null 이면 해당 항목은 변경하지 않음.
 *
 * - [level]: 1..MAX(20). 변경 시 서버가 stage·exp 를 자동으로 정합 상태로 맞춘다
 *   (stage=CharacterStage.forLevel, exp 는 새 임계치 미만이면 그대로 두고 초과면 0).
 * - [coin]: 0.. 절대값. (delta 가 아닌 절대값 set 으로 단순화 — 어드민 수동 보정 용도.)
 * - [dikoUnlocked]: 디코 해금 강제 토글. null 이면 변경 없음. 단, level>=10 이면 항상
 *   자동으로 true 가 유지되므로, false 로 내려도 다음 레벨업 시 자동 복구.
 */
@Schema(description = "어드민용 모찌 정보 수정 요청")
data class AdminUpdateCharacterRequest(

    @field:Min(1)
    @field:Max(20)
    @Schema(description = "변경할 레벨(1-20). 미전송 시 변경 없음.", example = "10")
    val level: Int? = null,

    @field:Min(0)
    @Schema(description = "변경할 코인 잔액 (절대값). 미전송 시 변경 없음.", example = "100")
    val coin: Int? = null,

    @Schema(description = "디코 해금 여부 강제 변경. 미전송 시 변경 없음.")
    val dikoUnlocked: Boolean? = null
)
