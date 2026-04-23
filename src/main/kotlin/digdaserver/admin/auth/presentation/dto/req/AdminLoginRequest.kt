package digdaserver.admin.auth.presentation.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

@Schema(description = "관리자 로그인 요청")
data class AdminLoginRequest(

    @field:NotBlank
    @field:Email
    @Schema(description = "관리자 이메일", example = "admin@digda.com")
    val email: String,

    @field:NotBlank
    @Schema(description = "관리자 비밀번호", example = "P@ssw0rd!")
    val password: String
)
