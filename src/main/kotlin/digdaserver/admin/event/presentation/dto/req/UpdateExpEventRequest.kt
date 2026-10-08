package digdaserver.admin.event.presentation.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@Schema(description = "모찌 경험치 배수 이벤트 설정 요청")
data class UpdateExpEventRequest(

    @Schema(description = "이벤트 켜기/끄기")
    val enabled: Boolean = false,

    @field:Size(max = 100)
    @Schema(description = "앱 배너 문구", example = "🌕 추석 맞이 모찌 경험치 2배!")
    val title: String = "",

    @field:DecimalMin("1.0")
    @field:DecimalMax("10.0")
    @Schema(description = "경험치 배수(1.0 ~ 10.0). 1.0 이면 이벤트 없음", example = "2.0")
    val multiplier: Double = 1.0,

    @Schema(description = "시작 시각(포함). 비우면 제한 없음", example = "2026-09-24T00:00:00")
    val startAt: LocalDateTime? = null,

    @Schema(description = "종료 시각(미포함). 비우면 제한 없음", example = "2026-10-01T00:00:00")
    val endAt: LocalDateTime? = null
)
