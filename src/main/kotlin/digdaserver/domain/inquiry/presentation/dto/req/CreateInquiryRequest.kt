package digdaserver.domain.inquiry.presentation.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "고객센터 문의 작성 요청")
data class CreateInquiryRequest(

    @field:NotBlank
    @field:Size(max = 1000)
    @Schema(description = "문의 내용", example = "로그인이 안 돼요.")
    val content: String
)
