package digdaserver.admin.nicknameexhibit.presentation.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "전시관 별명 카드 등록 요청")
data class CreateNicknameExhibitRequest(

    @field:NotBlank
    @field:Size(max = 100)
    @Schema(description = "별명", example = "디그다 박사")
    val nickname: String,

    @field:Size(max = 512)
    @Schema(description = "카드 앞면 이미지 URL (선택)")
    val imageUrl: String? = null,

    @field:NotBlank
    @Schema(description = "별명이 생긴 배경·역사·설명", example = "2024년 봄, 퀴즈를 가장 많이 만든 유저에게 붙은 별명")
    val history: String,

    @Schema(description = "목록 정렬 순서 (작을수록 앞). 미전송 시 0.", example = "0")
    val sortOrder: Int = 0
)
