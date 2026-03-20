package digdaserver.domain.user.domain.repository

import digdaserver.domain.user.domain.entity.UserPrivacySetting
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface UserPrivacySettingRepository : JpaRepository<UserPrivacySetting, Long> {

    fun findByUserId(userId: Long): Optional<UserPrivacySetting>
}
