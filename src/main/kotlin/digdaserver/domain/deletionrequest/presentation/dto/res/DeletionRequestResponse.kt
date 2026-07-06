package digdaserver.domain.deletionrequest.presentation.dto.res

import digdaserver.domain.deletionrequest.domain.entity.DeletionRequest
import digdaserver.domain.deletionrequest.domain.entity.DeletionRequestStatus
import digdaserver.domain.deletionrequest.domain.entity.DeletionRequestType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "삭제 요청 접수 결과")
data class DeletionRequestResponse(

    @Schema(description = "요청 ID")
    val id: Long,

    @Schema(description = "요청 종류")
    val type: DeletionRequestType,

    @Schema(description = "처리 상태")
    val status: DeletionRequestStatus,

    @Schema(description = "접수 시각")
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(entity: DeletionRequest): DeletionRequestResponse = DeletionRequestResponse(
            id = entity.id,
            type = entity.type,
            status = entity.status,
            createdAt = entity.createdAt
        )
    }
}
