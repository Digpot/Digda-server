package digdaserver.admin.deletionrequest.presentation.dto.res

import digdaserver.domain.deletionrequest.domain.entity.DeletionRequest
import digdaserver.domain.deletionrequest.domain.entity.DeletionRequestStatus
import digdaserver.domain.deletionrequest.domain.entity.DeletionRequestType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "어드민용 삭제 요청")
data class AdminDeletionRequestResponse(

    @Schema(description = "요청 ID")
    val id: Long,

    @Schema(description = "요청 종류(ACCOUNT/DATA)")
    val type: DeletionRequestType,

    @Schema(description = "가입 이메일")
    val email: String,

    @Schema(description = "그룹방 이름(데이터 삭제 요청만)")
    val groupRoomName: String?,

    @Schema(description = "삭제 요청 데이터 설명(데이터 삭제 요청만)")
    val content: String?,

    @Schema(description = "처리 상태")
    val status: DeletionRequestStatus,

    @Schema(description = "접수 시각")
    val createdAt: LocalDateTime,

    @Schema(description = "처리 시각")
    val handledAt: LocalDateTime?
) {
    companion object {
        fun from(entity: DeletionRequest): AdminDeletionRequestResponse = AdminDeletionRequestResponse(
            id = entity.id,
            type = entity.type,
            email = entity.email,
            groupRoomName = entity.groupRoomName,
            content = entity.content,
            status = entity.status,
            createdAt = entity.createdAt,
            handledAt = entity.handledAt
        )
    }
}
