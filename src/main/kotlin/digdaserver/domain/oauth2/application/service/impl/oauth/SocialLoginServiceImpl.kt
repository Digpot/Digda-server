package digdaserver.domain.oauth2.application.service.impl.oauth

import digdaserver.domain.user.domain.entity.Role
import digdaserver.domain.user.domain.entity.User
import digdaserver.domain.user.domain.repository.UserRepository
import digdaserver.domain.oauth2.application.service.CreateAccessTokenAndRefreshTokenService
import digdaserver.domain.oauth2.application.service.OAuth2Service
import digdaserver.domain.oauth2.application.service.SocialLoginService
import digdaserver.domain.oauth2.domain.entity.SocialProvider
import digdaserver.domain.oauth2.presentation.dto.req.SocialTokenRequest
import digdaserver.domain.oauth2.presentation.dto.res.LoginToken
import digdaserver.domain.oauth2.presentation.dto.res.oatuh.KakaoTokenResponse
import digdaserver.domain.oauth2.presentation.dto.res.oatuh.KakaoUserResponse
import digdaserver.global.infra.exception.error.ErrorCode
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.jwt.domain.entity.SocialToken
import digdaserver.global.jwt.domain.repository.SocialTokenRepository
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
@Transactional
class SocialLoginServiceImpl(
    oauth2Services: List<OAuth2Service>,
    private val tokenService: CreateAccessTokenAndRefreshTokenService,
    private val userRepository: UserRepository,
    private val socialTokenRepository: SocialTokenRepository,

    @Value("\${oauth2.apple.profile}")
    private val appleProfile: String
) : SocialLoginService {

    private val log = LoggerFactory.getLogger(this::class.java)

    private val oauth2ServicesMap: Map<SocialProvider, OAuth2Service> =
        oauth2Services.associateBy { it.getProvider() }

    override fun loginWithToken(provider: SocialProvider, tokenRequest: SocialTokenRequest): LoginToken {
        val oauth2Service = getOAuth2Service(provider)

        log.info("소셜 로그인 시작: provider={}", provider)

        val isValidToken = if (provider == SocialProvider.APPLE) {
            tokenRequest.idToken?.let { oauth2Service.validateToken(it) }
        } else {
            oauth2Service.validateToken(tokenRequest.accessToken)
        }

        if (!isValidToken!!) {
            throw DigdaException(ErrorCode.INVALID_PARAMETER, "유효하지 않은 토큰입니다")
        }

        val userResponse: KakaoUserResponse =
            if (provider == SocialProvider.APPLE && tokenRequest.idToken != null) {
                oauth2Service.getUserInfoFromIdToken(tokenRequest.idToken)
            } else {
                oauth2Service.getUserInfo(tokenRequest.accessToken)
            }

        log.info("사용자 정보 획득 완료: userId={}, provider={}", userResponse.id, provider)

        val tokenResponse = oauth2Service.convertToTokenResponse(tokenRequest)

        // TODO: Auth API 구현 시 소셜 로그인 → User 생성/조회 로직 재설계 필요
        val user = findOrCreateUser(provider, userResponse)

        return tokenService.createAccessTokenAndRefreshToken(
            user.id.toString(),
            user.role,
            user.email
        )
    }

    override fun login(provider: SocialProvider, code: String): LoginToken {
        val oauth2Service = getOAuth2Service(provider)

        val tokenResponse = oauth2Service.getTokens(code)

        val accessToken = tokenResponse.accessToken
            ?: throw DigdaException(ErrorCode.INVALID_PARAMETER, "access token 없음")

        val userResponse = oauth2Service.getUserInfo(accessToken)

        val user = findOrCreateUser(provider, userResponse)

        return tokenService.createAccessTokenAndRefreshToken(
            user.id.toString(),
            user.role,
            user.email
        )
    }

    override fun getLoginUrl(provider: SocialProvider): String {
        val oauth2Service = getOAuth2Service(provider)
        val url = oauth2Service.getLoginUrl()
        log.info("생성된 로그인 URL: {}", url)
        return url
    }

    private fun getOAuth2Service(provider: SocialProvider): OAuth2Service {
        val service = oauth2ServicesMap[provider]
            ?: throw DigdaException(ErrorCode.INVALID_PARAMETER)

        log.info("OAuth2Service 조회 성공: {}", service::class.simpleName)
        return service
    }

    // TODO: Auth API 구현 시 전면 재설계 필요 (User 엔티티 구조 변경됨)
    private fun findOrCreateUser(
        provider: SocialProvider,
        userResponse: KakaoUserResponse
    ): User {
        val email = userResponse.getEmail()
            ?: throw DigdaException(ErrorCode.INVALID_PARAMETER, "이메일 정보가 없습니다")

        val existingUser = userRepository.findByEmail(email).orElse(null)

        if (existingUser != null) {
            return existingUser
        }

        val profileImage = if (provider == SocialProvider.APPLE) appleProfile else userResponse.getProfile()

        val newUser = User(
            email = email,
            name = userResponse.getName() ?: "사용자",
            profileImage = profileImage,
            socialProvider = provider,
            role = Role.USER
        )

        return userRepository.save(newUser)
    }
}
