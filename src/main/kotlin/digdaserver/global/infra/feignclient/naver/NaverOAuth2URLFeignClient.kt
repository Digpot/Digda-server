package digdaserver.global.infra.feignclient.naver

import digdaserver.domain.oauth2.presentation.dto.res.naver.NaverTokenResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(
    name = "NaverOAuth",
    url = "https://nid.naver.com"
)
interface NaverOAuth2URLFeignClient {

    @PostMapping(
        value = ["/oauth2.0/token"],
        consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE]
    )
    fun getAccessToken(
        @RequestParam("grant_type") grantType: String,
        @RequestParam("client_id") clientId: String,
        @RequestParam("client_secret") clientSecret: String,
        @RequestParam("redirect_uri") redirectUri: String,
        @RequestParam("code") code: String,
        @RequestParam("state") state: String
    ): NaverTokenResponse

    @PostMapping(
        value = ["/oauth2.0/token"],
        consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE]
    )
    fun refreshToken(
        @RequestParam("grant_type") grantType: String,
        @RequestParam("client_id") clientId: String,
        @RequestParam("client_secret") clientSecret: String,
        @RequestParam("refresh_token") refreshToken: String
    ): NaverTokenResponse
}
