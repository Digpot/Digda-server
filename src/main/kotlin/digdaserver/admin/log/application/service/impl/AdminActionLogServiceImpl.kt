package digdaserver.admin.log.application.service.impl

import digdaserver.admin.common.dto.res.AdminPageResponse
import digdaserver.admin.log.application.service.AdminActionLogService
import digdaserver.admin.log.domain.entity.AdminAction
import digdaserver.admin.log.domain.entity.AdminActionLog
import digdaserver.admin.log.domain.repository.AdminActionLogRepository
import digdaserver.admin.log.presentation.dto.res.AdminActionLogResponse
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
@Transactional(readOnly = true)
class AdminActionLogServiceImpl(
    private val adminActionLogRepository: AdminActionLogRepository
) : AdminActionLogService {

    @Transactional
    override fun record(
        actorId: UUID?,
        action: AdminAction,
        targetType: String?,
        targetId: String?,
        detail: String?
    ) {
        adminActionLogRepository.save(
            AdminActionLog(
                actorId = actorId,
                action = action,
                targetType = targetType,
                targetId = targetId,
                detail = detail?.take(500)
            )
        )
    }

    override fun search(
        actorId: UUID?,
        action: AdminAction?,
        from: LocalDateTime?,
        to: LocalDateTime?,
        keyword: String?,
        page: Int,
        size: Int
    ): AdminPageResponse<AdminActionLogResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val result = adminActionLogRepository.searchLogs(actorId, action, from, to, keyword, pageable)
        return AdminPageResponse.of(result, AdminActionLogResponse::from)
    }
}
