package digdaserver.admin.grouproom.presentation.dto.res

import digdaserver.domain.group_room.domain.entity.GroupRoom
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "어드민용 그룹방 정보")
data class AdminGroupRoomResponse(

    @Schema(description = "그룹방 ID")
    val groupRoomId: Long,

    @Schema(description = "그룹방 이름")
    val name: String,

    @Schema(description = "썸네일 이미지 URL")
    val thumbnailImage: String?,

    @Schema(description = "최대 인원")
    val maxMembers: Int,

    @Schema(description = "방장 ID(UUID)")
    val ownerId: String,

    @Schema(description = "방장 이름")
    val ownerName: String,

    @Schema(description = "마지막 활동 시각")
    val lastActivityAt: LocalDateTime,

    @Schema(description = "삭제 예약 시각")
    val deleteScheduledAt: LocalDateTime?,

    @Schema(description = "삭제 시각")
    val deletedAt: LocalDateTime?,

    @Schema(description = "생성 시각")
    val createdAt: LocalDateTime,

    @Schema(description = "수정 시각")
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(room: GroupRoom): AdminGroupRoomResponse {
            // 실제 방장은 OWNER 역할 멤버십이 소스오브트루스다. 과거 방장 양도 시
            // group_room.owner_id 를 갱신하지 않던 버그로 남은 stale 데이터까지
            // 올바르게 보이도록, OWNER 멤버십 유저를 우선 사용하고 없으면 owner 로 폴백.
            val ownerUser = room.memberships.firstOrNull { it.isOwner }?.user ?: room.owner
            return AdminGroupRoomResponse(
                groupRoomId = room.id,
                name = room.name,
                thumbnailImage = room.thumbnailImage,
                maxMembers = room.maxMembers,
                ownerId = ownerUser.id.toString(),
                ownerName = ownerUser.name,
                lastActivityAt = room.lastActivityAt,
                deleteScheduledAt = room.deleteScheduledAt,
                deletedAt = room.deletedAt,
                createdAt = room.createdAt,
                updatedAt = room.updatedAt
            )
        }
    }
}
