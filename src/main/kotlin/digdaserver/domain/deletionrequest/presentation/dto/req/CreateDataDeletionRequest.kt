package digdaserver.domain.deletionrequest.presentation.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "데이터 삭제 요청(비로그인)")
data class CreateDataDeletionRequest(

    @field:NotBlank
    @field:Email
    @field:Size(max = 255)
    @Schema(description = "가입에 사용한 이메일", example = "user@example.com")
    val email: String,

    @field:NotBlank
    @field:Size(max = 255)
    @Schema(description = "데이터가 있는 그룹방 이름", example = "우리 가족 다이어리")
    val groupRoomName: String,

    @field:NotBlank
    @field:Size(max = 2000)
    @Schema(description = "삭제를 원하는 데이터 설명", example = "2026년 5월에 작성한 일기 전체")
    val content: String
)
