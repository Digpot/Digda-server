package digdaserver.domain.oauth2.application.service.impl.oauth

import digdaserver.domain.oauth2.application.service.OAuth2Service
import digdaserver.domain.oauth2.domain.entity.SocialProvider
import digdaserver.domain.oauth2.presentation.dto.req.SocialTokenRequest
import digdaserver.domain.oauth2.presentation.dto.res.naver.NaverTokenResponse
import digdaserver.domain.oauth2.presentation.dto.res.oatuh.KakaoTokenResponse
import digdaserver.domain.oauth2.presentation.dto.res.oatuh.KakaoUserResponse
import digdaserver.global.infra.feignclient.naver.NaverOAuth2URLFeignClient
import digdaserver.global.infra.feignclient.naver.NaverOAuth2UserFeignClient
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID

@Service
@Transactional
class NaverOAuth2ServiceImpl(
    private val naverOAuth2URLFeignClient: NaverOAuth2URLFeignClient,
    private val naverOAuth2UserFeignClient: NaverOAuth2UserFeignClient,

    @Value("\${oauth2.naver.client-id}")
    private val clientId: String,

    @Value("\${oauth2.naver.client-secret}")
    private val clientSecret: String,

    @Value("\${oauth2.naver.redirect-uri}")
    private val redirectUri: String,

    @Value("\${oauth2.naver.base-url}")
    private val baseUrl: String
) : OAuth2Service {

    override fun getLoginUrl(): String {
        val state = generateState()

        return "$baseUrl?response_type=code" +
            "&client_id=$clientId" +
            "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
            "&state=$state"
    }

    override fun getTokens(code: String): KakaoTokenResponse {
        val response: NaverTokenResponse = naverOAuth2URLFeignClient.getAccessToken(
            "authorization_code",
            clientId,
            clientSecret,
            redirectUri,
            code,
            "state_value"
        )

        return KakaoTokenResponse(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
            idToken = "idnull",
            expiresIn = response.expiresIn?.toLong()
        )
    }

    override fun refreshTokens(refreshToken: String): KakaoTokenResponse {
        val response: NaverTokenResponse = naverOAuth2URLFeignClient.refreshToken(
            "refresh_token",
            clientId,
            clientSecret,
            refreshToken
        )

        return KakaoTokenResponse(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
            idToken = "idnull",
            expiresIn = response.expiresIn?.toLong()
        )
    }

    override fun getUserInfo(accessToken: String): KakaoUserResponse {
        TODO("Not yet implemented")
    }

    /*override fun getUserInfo(accessToken: String): KakaoUserResponse {
        return try {
            naverOAuth2UserFeignClient.getUserInfo("Bearer $accessToken")
        } catch (e: Exception) {
            log.error("카카오 사용자 정보 조회 실패: {}", e.message)
            throw DigdaException(ErrorCode.INVALID_PROVIDER)
        }
    }*/

    override fun getUserInfoFromIdToken(idToken: String): KakaoUserResponse {
        throw UnsupportedOperationException("네이버는 ID Token을 지원하지 않습니다")
    }

    override fun validateToken(accessToken: String): Boolean {
        return try {
            val userInfo = getUserInfo(accessToken)
            userInfo.id != null
        } catch (e: Exception) {
            false
        }
    }

    override fun convertToTokenResponse(tokenRequest: SocialTokenRequest): KakaoTokenResponse {
        return KakaoTokenResponse(
            accessToken = tokenRequest.accessToken,
            refreshToken = tokenRequest.refreshToken,
            idToken = "idnull",
            expiresIn = tokenRequest.expiresIn
        )
    }

    override fun getProvider(): SocialProvider = SocialProvider.NAVER

    private fun generateState(): String = UUID.randomUUID().toString()
}
