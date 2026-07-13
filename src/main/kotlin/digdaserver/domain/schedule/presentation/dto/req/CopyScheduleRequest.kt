package digdaserver.domain.schedule.presentation.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "일정 여러 날짜 복사 요청")
data class CopyScheduleRequest(

    @Schema(description = "복사본을 만들 시작 날짜 목록 (최대 31개, 중복 제거됨)", example = "[\"2026-07-16\", \"2026-07-17\"]")
    val dates: List<LocalDate>
)
