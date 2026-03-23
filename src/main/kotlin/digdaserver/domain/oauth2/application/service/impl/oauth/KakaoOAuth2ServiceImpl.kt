package digdaserver.domain.oauth2.application.service.impl.oauth

import digdaserver.domain.oauth2.application.service.OAuth2Service
import digdaserver.domain.oauth2.domain.entity.SocialProvider
import digdaserver.domain.oauth2.presentation.dto.req.SocialTokenRequest
import digdaserver.domain.oauth2.presentation.dto.res.oatuh.KakaoTokenResponse
import digdaserver.domain.oauth2.presentation.dto.res.oatuh.KakaoUserResponse
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import digdaserver.global.infra.feignclient.kakao.KakaoOAuth2URLFeignClient
import digdaserver.global.infra.feignclient.kakao.KakaoOAuth2UserFeignClient
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
@Transactional
class KakaoOAuth2ServiceImpl(
    private val kakaoOAuth2URLFeignClient: KakaoOAuth2URLFeignClient,
    private val kakaoOAuth2UserFeignClient: KakaoOAuth2UserFeignClient,

    @Value("\${oauth2.kakao.client-id}")
    private val clientId: String,

    @Value("\${oauth2.kakao.client-secret}")
    private val clientSecret: String,

    @Value("\${oauth2.kakao.redirect-uri}")
    private val redirectUri: String,

    @Value("\${oauth2.kakao.base-url}")
    private val baseUrl: String
) : OAuth2Service {

    private val log = LoggerFactory.getLogger(KakaoOAuth2ServiceImpl::class.java)

    override fun getLoginUrl(): String {
        log.info("카카오 로그인 URL 생성: clientId={}, redirectUri={}", clientId, redirectUri)
        return "$baseUrl?client_id=$clientId&redirect_uri=$redirectUri&response_type=code&scope=profile_nickname,profile_image,account_email"
    }

    override fun getTokens(code: String): KakaoTokenResponse {
        return kakaoOAuth2URLFeignClient.getAccessToken(
            code,
            clientId,
            clientSecret,
            redirectUri,
            "authorization_code"
        )
    }

    override fun refreshTokens(refreshToken: String): KakaoTokenResponse {
        return kakaoOAuth2URLFeignClient.refreshToken(
            "refresh_token",
            refreshToken,
            clientId,
            clientSecret
        )
    }

    override fun getUserInfo(accessToken: String): KakaoUserResponse {
        return try {
            kakaoOAuth2UserFeignClient.getUserInfo("Bearer $accessToken")
        } catch (e: Exception) {
            log.error("카카오 사용자 정보 조회 실패: {}", e.message)
            throw DigdaException(ErrorCode.INVALID_PROVIDER)
        }
    }

    override fun getUserInfoFromIdToken(idToken: String): KakaoUserResponse {
        throw UnsupportedOperationException("카카오는 ID Token을 지원하지 않습니다.")
    }

    override fun validateToken(accessToken: String): Boolean {
        return try {
            val userInfo = getUserInfo(accessToken)
            userInfo.id != null
        } catch (e: Exception) {
            log.error("카카오 토큰 검증 실패: {}", e.message)
            false
        }
    }

    override fun convertToTokenResponse(tokenRequest: SocialTokenRequest): KakaoTokenResponse {
        return KakaoTokenResponse(
            tokenRequest.accessToken,
            tokenRequest.refreshToken,
            "idnull",
            tokenRequest.expiresIn
        )
    }

    override fun getProvider(): SocialProvider {
        return SocialProvider.KAKAO
    }
}
