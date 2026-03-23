package digdaserver.domain.oauth2.application.service.impl

import digdaserver.domain.oauth2.application.service.CreateAccessTokenAndRefreshTokenService
import digdaserver.domain.oauth2.presentation.dto.res.LoginToken
import digdaserver.domain.user.domain.entity.Role
import digdaserver.domain.user.domain.repository.UserRepository
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

        val user = userRepository.findById(userId.toLong()).orElse(null)
        val isNewUser = user?.terms == null

        return LoginToken.of(accessToken, refreshToken, isNewUser)
    }
}
