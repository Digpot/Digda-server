package digdaserver.domain.event.presentation.dto.res

import digdaserver.domain.event.domain.entity.ExpEvent
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * 경험치 배수 이벤트 상태. 앱과 어드민이 같은 DTO 를 본다.
 *
 * 앱은 [active] 와 [title] 만 보고 배너를 띄우면 되고, 어드민은 [enabled] / 기간까지 함께
 * 편집한다. ([enabled]=true 여도 기간 밖이면 [active]=false 이므로 둘을 구분해 내려준다.)
 */
@Schema(description = "모찌 경험치 배수 이벤트")
data class ExpEventResponse(

    @Schema(description = "이벤트 스위치(어드민 설정값)")
    val enabled: Boolean,

    @Schema(description = "지금 실제로 배수가 적용되는 중인지 — enabled && 기간 안 && 배수>1")
    val active: Boolean,

    @Schema(description = "앱 배너 문구", example = "🌕 추석 맞이 모찌 경험치 2배!")
    val title: String,

    @Schema(description = "설정된 배수", example = "2.0")
    val multiplier: Double,

    @Schema(description = "지금 적용 중인 배수 — 비활성이면 1.0", example = "2.0")
    val appliedMultiplier: Double,

    @Schema(description = "시작 시각(포함). null = 제한 없음")
    val startAt: LocalDateTime?,

    @Schema(description = "종료 시각(미포함). null = 제한 없음")
    val endAt: LocalDateTime?
) {
    companion object {
        fun from(event: ExpEvent, now: LocalDateTime): ExpEventResponse = ExpEventResponse(
            enabled = event.enabled,
            active = event.isActiveAt(now),
            title = event.title,
            multiplier = event.multiplier,
            appliedMultiplier = event.multiplierAt(now),
            startAt = event.startAt,
            endAt = event.endAt
        )

        /** 아직 이벤트 행이 없는 환경(신규 DB)에서 내려줄 기본값 — 이벤트 없음. */
        val default: ExpEventResponse = ExpEventResponse(
            enabled = false,
            active = false,
            title = "",
            multiplier = 1.0,
            appliedMultiplier = 1.0,
            startAt = null,
            endAt = null
        )
    }
}
