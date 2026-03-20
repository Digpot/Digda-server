package digdaserver.domain.oauth2.application.service.impl.auth

import digdaserver.domain.oauth2.application.service.LogoutService
import digdaserver.global.infra.exception.error.ErrorCode
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.jwt.domain.repository.JsonWebTokenRepository
import digdaserver.global.jwt.domain.repository.SocialTokenRepository
import digdaserver.global.jwt.util.JWTUtil
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@Transactional
class LogoutServiceImpl(
    private val jwtUtil: JWTUtil,
    private val jsonWebTokenRepository: JsonWebTokenRepository,
    private val socialTokenRepository: SocialTokenRepository
) : LogoutService {

    private val log = LoggerFactory.getLogger(LogoutServiceImpl::class.java)

    override fun logout(userId: String, refreshToken: String) {
        if (!jwtUtil.jwtVerify(refreshToken, "refresh")) {
            throw DigdaException(ErrorCode.TOKEN_INVALID)
        }

        val jsonWebToken = jsonWebTokenRepository.findById(refreshToken)
            .orElseThrow { DigdaException(ErrorCode.REFRESH_TOKEN_NOT_FOUND) }

        socialTokenRepository.deleteByUserId(jsonWebToken.providerId)
        jsonWebTokenRepository.delete(jsonWebToken)

        log.info("사용자 로그아웃 완료: userId={}", jsonWebToken.providerId)
    }
}
