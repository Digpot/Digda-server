package digdaserver.domain.oauth2.application.service.impl.auth

import digdaserver.domain.user.domain.entity.Role
import digdaserver.domain.user.domain.entity.User
import digdaserver.domain.user.domain.repository.UserRepository
import digdaserver.domain.oauth2.application.service.ReissueService
import digdaserver.domain.oauth2.presentation.dto.res.LoginToken
import digdaserver.global.infra.exception.error.ErrorCode
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.jwt.domain.entity.JsonWebToken
import digdaserver.global.jwt.domain.repository.JsonWebTokenRepository
import digdaserver.global.jwt.util.JWTUtil
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@Transactional
class ReissueServiceImpl(
    private val jwtUtil: JWTUtil,
    private val jsonWebTokenRepository: JsonWebTokenRepository,
    private val userRepository: UserRepository
) : ReissueService {

    private val log = LoggerFactory.getLogger(ReissueServiceImpl::class.java)

    override fun reissue(refreshToken: String): LoginToken {
        if (!jwtUtil.jwtVerify(refreshToken, "refresh")) {
            log.info("Refresh token not valid")
            throw DigdaException(ErrorCode.REFRESH_TOKEN_INVALID)
        }

        val jsonWebToken = jsonWebTokenRepository.findById(refreshToken)
            .orElseThrow { DigdaException(ErrorCode.REFRESH_TOKEN_NOT_FOUND) }

        val userId = jsonWebToken.providerId
        val role: Role = jsonWebToken.role
        val email = jsonWebToken.email

        val newAccessToken = jwtUtil.createAccessToken(userId, role, email)
        val newRefreshToken = jwtUtil.createRefreshToken(userId, role, email)

        val newJsonWebToken = JsonWebToken.of(
            refreshToken = refreshToken,
            providerId = userId,
            email = email,
            role = role
        )

        jsonWebTokenRepository.delete(jsonWebToken)
        jsonWebTokenRepository.save(newJsonWebToken)

        return LoginToken.of(newAccessToken, newRefreshToken, onBoarding(userId))
    }

    // TODO: Auth API 구현 시 온보딩 로직 재설계 필요 (약관 동의 여부 기반)
    fun onBoarding(userId: String): Boolean {
        return false
    }
}
