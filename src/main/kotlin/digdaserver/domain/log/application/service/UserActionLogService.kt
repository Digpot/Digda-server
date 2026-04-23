package digdaserver.domain.log.application.service

import digdaserver.domain.log.domain.entity.UserAction
import digdaserver.domain.log.domain.entity.UserActionLog
import org.springframework.data.domain.Page
import java.time.LocalDateTime
import java.util.UUID

interface UserActionLogService {

    fun record(
        actorId: UUID?,
        action: UserAction,
        targetType: String?,
        targetId: String?,
        detail: String?
    )

    fun search(
        actorId: UUID?,
        action: UserAction?,
        from: LocalDateTime?,
        to: LocalDateTime?,
        keyword: String?,
        page: Int,
        size: Int
    ): Page<UserActionLog>
}
