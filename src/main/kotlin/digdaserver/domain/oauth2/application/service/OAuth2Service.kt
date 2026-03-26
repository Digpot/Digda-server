package digdaserver.domain.oauth2.application.service

import digdaserver.domain.oauth2.domain.entity.SocialProvider
import digdaserver.domain.oauth2.presentation.dto.req.SocialTokenRequest
import digdaserver.domain.oauth2.presentation.dto.res.oauth.OAuthTokenResponse
import digdaserver.domain.oauth2.presentation.dto.res.oauth.OAuthUserResponse

interface OAuth2Service {

    // 기존 Authorization Code Flow 방식
    fun getLoginUrl(): String
    fun getTokens(code: String): OAuthTokenResponse
    fun refreshTokens(refreshToken: String): OAuthTokenResponse

    // Access Token → 사용자 정보 조회
    fun getUserInfo(accessToken: String): OAuthUserResponse

    // Apple: ID Token 기반 사용자 정보 조회
    fun getUserInfoFromIdToken(idToken: String): OAuthUserResponse

    // 토큰 검증
    fun validateToken(accessToken: String): Boolean

    // SocialTokenRequest → OAuthTokenResponse 변환
    fun convertToTokenResponse(tokenRequest: SocialTokenRequest): OAuthTokenResponse

    fun getProvider(): SocialProvider
}
