package digdaserver.domain.log.application.service.impl

import digdaserver.domain.log.application.service.UserActionLogService
import digdaserver.domain.log.domain.entity.UserAction
import digdaserver.domain.log.domain.entity.UserActionLog
import digdaserver.domain.log.domain.repository.UserActionLogRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
@Transactional(readOnly = true)
class UserActionLogServiceImpl(
    private val userActionLogRepository: UserActionLogRepository
) : UserActionLogService {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun record(
        actorId: UUID?,
        action: UserAction,
        targetType: String?,
        targetId: String?,
        detail: String?
    ) {
        userActionLogRepository.save(
            UserActionLog(
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
        action: UserAction?,
        from: LocalDateTime?,
        to: LocalDateTime?,
        keyword: String?,
        page: Int,
        size: Int
    ): Page<UserActionLog> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        return userActionLogRepository.searchLogs(actorId, action, from, to, keyword, pageable)
    }
}
