package digdaserver.domain.user.domain.repository

import digdaserver.domain.user.domain.entity.UserNotificationSetting
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface UserNotificationSettingRepository : JpaRepository<UserNotificationSetting, Long> {

    fun findByUserId(userId: UUID): Optional<UserNotificationSetting>
}
