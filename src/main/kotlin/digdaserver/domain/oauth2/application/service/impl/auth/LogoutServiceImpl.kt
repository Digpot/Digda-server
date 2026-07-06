package digdaserver.domain.oauth2.application.service.impl.auth

import digdaserver.domain.device.domain.repository.DeviceRepository
import digdaserver.domain.log.application.service.UserActionLogService
import digdaserver.domain.log.domain.entity.UserAction
import digdaserver.domain.oauth2.application.service.LogoutService
import digdaserver.global.jwt.domain.repository.JsonWebTokenRepository
import digdaserver.global.jwt.domain.repository.SocialTokenRepository
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@Transactional
class LogoutServiceImpl(
    private val jsonWebTokenRepository: JsonWebTokenRepository,
    private val socialTokenRepository: SocialTokenRepository,
    private val deviceRepository: DeviceRepository,
    private val userActionLogService: UserActionLogService
) : LogoutService {

    private val log = LoggerFactory.getLogger(LogoutServiceImpl::class.java)

    override fun logout(userId: String) {
        jsonWebTokenRepository.deleteByProviderId(userId)
        socialTokenRepository.deleteByUserId(userId)

        val userUuid = runCatching { UUID.fromString(userId) }.getOrNull()
        if (userUuid != null) {
            deviceRepository.deleteAllByUserId(userUuid)
        }

        userActionLogService.record(
            actorId = userUuid,
            action = UserAction.LOGOUT,
            targetType = "USER",
            targetId = userId,
            detail = null
        )

        log.info("사용자 로그아웃 완료: userId={}", userId)
    }
}
