package digdaserver.domain.inquiry.presentation.dto.res

import digdaserver.domain.inquiry.domain.entity.Inquiry
import digdaserver.domain.inquiry.domain.entity.InquiryStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "고객센터 문의")
data class InquiryResponse(

    @Schema(description = "문의 ID")
    val id: Long,

    @Schema(description = "문의 내용")
    val content: String,

    @Schema(description = "처리 상태")
    val status: InquiryStatus,

    @Schema(description = "접수 시각")
    val createdAt: LocalDateTime,

    @Schema(description = "답변 시각")
    val answeredAt: LocalDateTime?
) {
    companion object {
        fun from(inquiry: Inquiry): InquiryResponse = InquiryResponse(
            id = inquiry.id,
            content = inquiry.content,
            status = inquiry.status,
            createdAt = inquiry.createdAt,
            answeredAt = inquiry.answeredAt
        )
    }
}
