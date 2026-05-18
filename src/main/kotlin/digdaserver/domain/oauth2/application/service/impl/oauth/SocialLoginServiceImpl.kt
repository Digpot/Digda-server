package digdaserver.domain.oauth2.application.service.impl.oauth

import digdaserver.domain.log.application.service.UserActionLogService
import digdaserver.domain.log.domain.entity.UserAction
import digdaserver.domain.oauth2.application.service.CreateAccessTokenAndRefreshTokenService
import digdaserver.domain.oauth2.application.service.OAuth2Service
import digdaserver.domain.oauth2.application.service.SocialLoginService
import digdaserver.domain.oauth2.domain.entity.SocialProvider
import digdaserver.domain.oauth2.presentation.dto.req.LoginRequest
import digdaserver.domain.oauth2.presentation.dto.req.SocialTokenRequest
import digdaserver.domain.oauth2.presentation.dto.res.LoginResponse
import digdaserver.domain.oauth2.presentation.dto.res.LoginToken
import digdaserver.domain.oauth2.presentation.dto.res.UserResponse
import digdaserver.domain.oauth2.presentation.dto.res.oauth.OAuthUserResponse
import digdaserver.domain.user.domain.entity.Role
import digdaserver.domain.user.domain.entity.User
import digdaserver.domain.user.domain.entity.UserNotificationSetting
import digdaserver.domain.user.domain.entity.UserPrivacySetting
import digdaserver.domain.user.domain.repository.UserRepository
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
@Transactional
class SocialLoginServiceImpl(
    oauth2Services: List<OAuth2Service>,
    private val tokenService: CreateAccessTokenAndRefreshTokenService,
    private val userRepository: UserRepository,
    private val userActionLogService: UserActionLogService,

    @Value("\${oauth2.apple.profile}")
    private val appleProfile: String
) : SocialLoginService {

    private val log = LoggerFactory.getLogger(this::class.java)

    private val oauth2ServicesMap: Map<SocialProvider, OAuth2Service> =
        oauth2Services.associateBy { it.getProvider() }

    override fun login(request: LoginRequest): LoginResponse {
        val provider = SocialProvider.from(request.provider)
        val oauth2Service = getOAuth2Service(provider)

        log.info("소셜 로그인 시작: provider={}", provider)

        val isValidToken = if (provider == SocialProvider.APPLE) {
            request.idToken?.let { oauth2Service.validateToken(it) }
                ?: throw DigdaException(ErrorCode.INVALID_PARAMETER, "Apple 로그인 시 idToken은 필수입니다")
        } else {
            oauth2Service.validateToken(request.accessToken)
        }

        if (!isValidToken) {
            throw DigdaException(ErrorCode.SOCIAL_AUTH_FAILED)
        }

        val userResponse: OAuthUserResponse =
            if (provider == SocialProvider.APPLE && request.idToken != null) {
                oauth2Service.getUserInfoFromIdToken(request.idToken)
            } else {
                oauth2Service.getUserInfo(request.accessToken)
            }

        log.info("사용자 정보 획득 완료: provider={}", provider)

        val (user, isNewUser) = findOrCreateUser(provider, userResponse)

        val loginToken = tokenService.createAccessTokenAndRefreshToken(
            user.id.toString(),
            user.role,
            user.email
        )

        userActionLogService.record(
            actorId = user.id,
            action = if (isNewUser) UserAction.SIGNUP else UserAction.LOGIN,
            targetType = "USER",
            targetId = user.id.toString(),
            detail = "provider=$provider, email=${user.email}"
        )

        log.info("userId={}, action={}, provider={}",
            user.id,
            if (isNewUser) "회원 가입" else "로그인",
            provider)

        return LoginResponse(
            accessToken = loginToken.accessToken,
            refreshToken = loginToken.refreshToken,
            user = UserResponse.from(user),
            isNewUser = isNewUser
        )
    }

    override fun loginWithToken(provider: SocialProvider, tokenRequest: SocialTokenRequest): LoginToken {
        val loginRequest = LoginRequest(
            provider = provider.value,
            accessToken = tokenRequest.accessToken,
            idToken = tokenRequest.idToken
        )
        val response = login(loginRequest)
        return LoginToken.of(response.accessToken, response.refreshToken, response.isNewUser)
    }

    override fun loginWithCode(provider: SocialProvider, code: String): LoginToken {
        val oauth2Service = getOAuth2Service(provider)

        val tokenResponse = oauth2Service.getTokens(code)

        val userResponse = if (provider == SocialProvider.APPLE) {
            val idToken = tokenResponse.idToken
                ?: throw DigdaException(ErrorCode.INVALID_PARAMETER, "Apple ID Token 없음")
            oauth2Service.getUserInfoFromIdToken(idToken)
        } else {
            val accessToken = tokenResponse.accessToken
                ?: throw DigdaException(ErrorCode.INVALID_PARAMETER, "access token 없음")
            oauth2Service.getUserInfo(accessToken)
        }

        val (user, isNewUser) = findOrCreateUser(provider, userResponse)

        userActionLogService.record(
            actorId = user.id,
            action = if (isNewUser) UserAction.SIGNUP else UserAction.LOGIN,
            targetType = "USER",
            targetId = user.id.toString(),
            detail = "provider=$provider, email=${user.email}"
        )

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
            ?: throw DigdaException(ErrorCode.INVALID_PROVIDER)

        log.info("OAuth2Service 조회 성공: {}", service::class.simpleName)
        return service
    }

    private fun findOrCreateUser(
        provider: SocialProvider,
        userResponse: OAuthUserResponse
    ): Pair<User, Boolean> {
        val socialId = userResponse.id
            ?: throw DigdaException(ErrorCode.INVALID_PARAMETER, "소셜 고유 ID가 없습니다")

        val existingUser = userRepository.findBySocialIdAndSocialProvider(socialId, provider).orElse(null)

        if (existingUser != null) {
            val isNewUser = existingUser.terms == null
            return Pair(existingUser, isNewUser)
        }

        val profileImage = if (provider == SocialProvider.APPLE) appleProfile else userResponse.getProfile()

        val newUser = User(
            socialId = socialId,
            email = userResponse.getEmail(),
            name = userResponse.getName() ?: "사용자",
            profileImage = profileImage,
            socialProvider = provider,
            role = Role.USER
        )

        newUser.initNotificationSetting(UserNotificationSetting(user = newUser))
        newUser.initPrivacySetting(UserPrivacySetting(user = newUser))

        val savedUser = userRepository.save(newUser)
        return Pair(savedUser, true)
    }
}
