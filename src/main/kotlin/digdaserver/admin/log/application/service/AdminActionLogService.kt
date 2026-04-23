package digdaserver.admin.log.application.service

import digdaserver.admin.common.dto.res.AdminPageResponse
import digdaserver.admin.log.domain.entity.AdminAction
import digdaserver.admin.log.presentation.dto.res.AdminActionLogResponse
import java.time.LocalDateTime
import java.util.UUID

interface AdminActionLogService {

    fun record(
        actorId: UUID?,
        action: AdminAction,
        targetType: String?,
        targetId: String?,
        detail: String?
    )

    fun search(
        actorId: UUID?,
        action: AdminAction?,
        from: LocalDateTime?,
        to: LocalDateTime?,
        keyword: String?,
        page: Int,
        size: Int
    ): AdminPageResponse<AdminActionLogResponse>
}
