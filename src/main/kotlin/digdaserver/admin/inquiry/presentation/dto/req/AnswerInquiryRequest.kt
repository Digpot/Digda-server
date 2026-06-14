package digdaserver.admin.inquiry.presentation.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "고객센터 문의 답변 등록 요청")
data class AnswerInquiryRequest(

    @field:NotBlank
    @field:Size(max = 2000)
    @Schema(description = "답변 내용", example = "안내드립니다. ...")
    val answer: String
)
