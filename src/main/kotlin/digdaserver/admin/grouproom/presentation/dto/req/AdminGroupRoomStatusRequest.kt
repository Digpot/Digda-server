package digdaserver.admin.grouproom.presentation.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

enum class GroupRoomAdminAction {
    RECOVER,
    SCHEDULE_DELETE,
    HARD_DELETE
}

@Schema(description = "그룹방 상태 변경 요청")
data class AdminGroupRoomStatusRequest(

    @field:NotNull
    @Schema(description = "액션: RECOVER(복구) / SCHEDULE_DELETE(삭제 예약) / HARD_DELETE(즉시 삭제)")
    val action: GroupRoomAdminAction
)
