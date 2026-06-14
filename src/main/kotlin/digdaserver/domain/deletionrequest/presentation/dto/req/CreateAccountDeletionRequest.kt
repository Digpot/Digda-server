package digdaserver.domain.deletionrequest.presentation.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "계정 삭제 요청(비로그인)")
data class CreateAccountDeletionRequest(

    @field:NotBlank
    @field:Email
    @field:Size(max = 255)
    @Schema(description = "가입에 사용한 이메일", example = "user@example.com")
    val email: String
)
