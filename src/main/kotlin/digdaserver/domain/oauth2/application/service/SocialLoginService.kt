package digdaserver.domain.oauth2.application.service

import digdaserver.domain.oauth2.domain.entity.SocialProvider
import digdaserver.domain.oauth2.presentation.dto.req.LoginRequest
import digdaserver.domain.oauth2.presentation.dto.req.SocialTokenRequest
import digdaserver.domain.oauth2.presentation.dto.res.LoginResponse
import digdaserver.domain.oauth2.presentation.dto.res.LoginToken

interface SocialLoginService {

    // 스펙 기반 소셜 로그인 (POST /auth/login)
    fun login(request: LoginRequest): LoginResponse

    // 앱에서 AccessToken / IDToken을 직접 전달받는 방식 (기존 호환)
    fun loginWithToken(provider: SocialProvider, tokenRequest: SocialTokenRequest): LoginToken

    // 기존 Authorization Code Flow 로그인 (테스트)
    fun loginWithCode(provider: SocialProvider, code: String): LoginToken

    // 로그인 URL 조회 (테스트)
    fun getLoginUrl(provider: SocialProvider): String
}
