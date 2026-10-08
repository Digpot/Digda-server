package digdaserver.admin.feedback.presentation.dto.res

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import digdaserver.global.common.masking.MaskedName
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "어드민용 피드백 응답 1건")
data class AdminFeedbackSubmissionResponse(
    val submissionId: Long,
    @Schema(description = "제출자 ID(UUID). 탈퇴 등으로 없으면 null") val userId: String?,
    @Schema(description = "제출자 이름. 없으면 null")
    @get:JsonSerialize(using = MaskedName::class)
    val userName: String?,
    @Schema(description = "문항별 응답(제출 시점 스냅샷)") val answers: List<Answer>,
    val createdAt: LocalDateTime
) {
    data class Answer(
        val questionId: Long?,
        val title: String,
        val type: String,
        val answer: String
    )
}
