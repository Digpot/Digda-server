package digdaserver.global.infra.feignclient.kakao

import digdaserver.domain.oauth2.presentation.dto.res.oauth.OAuthTokenResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(
    name = "KakaoOAuth",
    url = "https://kauth.kakao.com"
)
interface KakaoOAuth2URLFeignClient {

    @PostMapping(
        value = ["/oauth/token"],
        consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE]
    )
    fun getAccessToken(
        @RequestParam("code") code: String,
        @RequestParam("client_id") clientId: String,
        @RequestParam("client_secret") clientSecret: String,
        @RequestParam("redirect_uri") redirectUri: String,
        @RequestParam("grant_type") grantType: String
    ): OAuthTokenResponse

    @PostMapping(
        value = ["/oauth/token"],
        consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE]
    )
    fun refreshToken(
        @RequestParam("grant_type") grantType: String,
        @RequestParam("refresh_token") refreshToken: String,
        @RequestParam("client_id") clientId: String,
        @RequestParam("client_secret") clientSecret: String
    ): OAuthTokenResponse
}
