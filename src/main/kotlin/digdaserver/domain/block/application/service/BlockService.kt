package digdaserver.domain.block.application.service

import digdaserver.domain.block.domain.entity.HideReason
import digdaserver.domain.block.domain.entity.HideTargetType
import digdaserver.domain.block.presentation.dto.res.BlockedUserResponse
import java.util.UUID

interface BlockService {

    /** 사용자 단위 차단(전역·단방향). 멱등 — 이미 차단 중이면 무시. */
    fun blockUser(blockerId: UUID, blockedUserId: UUID)

    /** 차단 해제. 멱등 — 차단 중이 아니면 무시. */
    fun unblockUser(blockerId: UUID, blockedUserId: UUID)

    /** 마이페이지 차단 목록(최신순). */
    fun listBlockedUsers(blockerId: UUID): List<BlockedUserResponse>

    /** 개별 콘텐츠 숨김(본인에게서만). 신고 자동 숨김도 이걸 [HideReason.REPORTED] 로 호출. 멱등. */
    fun hideContent(userId: UUID, targetType: HideTargetType, targetId: Long, reason: HideReason)

    /** 개별 콘텐츠 숨김 해제. 멱등. */
    fun unhideContent(userId: UUID, targetType: HideTargetType, targetId: Long)
}
