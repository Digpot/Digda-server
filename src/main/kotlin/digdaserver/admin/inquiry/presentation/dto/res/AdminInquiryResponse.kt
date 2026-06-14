package digdaserver.admin.inquiry.presentation.dto.res

import digdaserver.domain.inquiry.domain.entity.Inquiry
import digdaserver.domain.inquiry.domain.entity.InquiryStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "어드민용 고객센터 문의")
data class AdminInquiryResponse(

    @Schema(description = "문의 ID")
    val inquiryId: Long,

    @Schema(description = "작성자 ID(UUID)")
    val userId: String,

    @Schema(description = "작성자 이름")
    val userName: String,

    @Schema(description = "문의 내용")
    val content: String,

    @Schema(description = "처리 상태")
    val status: InquiryStatus,

    @Schema(description = "어드민 답변 내용(미답변이면 null)")
    val answer: String?,

    @Schema(description = "접수 시각")
    val createdAt: LocalDateTime,

    @Schema(description = "답변 시각")
    val answeredAt: LocalDateTime?
) {
    companion object {
        fun from(inquiry: Inquiry): AdminInquiryResponse = AdminInquiryResponse(
            inquiryId = inquiry.id,
            userId = inquiry.user.id.toString(),
            userName = inquiry.user.displayedName(),
            content = inquiry.content,
            status = inquiry.status,
            answer = inquiry.answer,
            createdAt = inquiry.createdAt,
            answeredAt = inquiry.answeredAt
        )
    }
}
