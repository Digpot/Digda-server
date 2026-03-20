package digdaserver.domain.oauth2.application.service.impl

import digdaserver.domain.user.domain.entity.Role
import digdaserver.domain.user.domain.entity.User
import digdaserver.domain.user.domain.repository.UserRepository
import digdaserver.domain.oauth2.application.service.CreateAccessTokenAndRefreshTokenService
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
class CreateAccessTokenAndRefreshTokenServiceImpl(
    private val jwtUtil: JWTUtil,
    private val jsonWebTokenRepository: JsonWebTokenRepository,
    private val userRepository: UserRepository
) : CreateAccessTokenAndRefreshTokenService {

    private val log = LoggerFactory.getLogger(CreateAccessTokenAndRefreshTokenServiceImpl::class.java)

    override fun createAccessTokenAndRefreshToken(
        userId: String,
        role: Role,
        email: String
    ): LoginToken {
        val accessToken = jwtUtil.createAccessToken(userId, role, email)
        val refreshToken = jwtUtil.createRefreshToken(userId, role, email)

        val jsonWebToken = JsonWebToken.of(
            refreshToken = refreshToken,
            providerId = userId,
            email = email,
            role = role
        )

        jsonWebTokenRepository.save(jsonWebToken)

        val nameFlag = onBoarding(userId)

        return LoginToken.of(accessToken, refreshToken, nameFlag)
    }

    // TODO: Auth API 구현 시 온보딩 로직 재설계 필요 (약관 동의 여부 기반)
    private fun onBoarding(userId: String): Boolean {
        return false
    }
}
