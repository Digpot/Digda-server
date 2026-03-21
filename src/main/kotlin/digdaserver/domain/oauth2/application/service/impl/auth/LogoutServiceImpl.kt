package digdaserver.domain.oauth2.application.service.impl.auth

import digdaserver.domain.oauth2.application.service.LogoutService
import digdaserver.global.jwt.domain.repository.JsonWebTokenRepository
import digdaserver.global.jwt.domain.repository.SocialTokenRepository
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@Transactional
class LogoutServiceImpl(
    private val jsonWebTokenRepository: JsonWebTokenRepository,
    private val socialTokenRepository: SocialTokenRepository
) : LogoutService {

    private val log = LoggerFactory.getLogger(LogoutServiceImpl::class.java)

    override fun logout(userId: String) {
        jsonWebTokenRepository.deleteByProviderId(userId)
        socialTokenRepository.deleteByUserId(userId)

        log.info("사용자 로그아웃 완료: userId={}", userId)
    }
}
