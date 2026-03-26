package digdaserver.global.infra.feignclient.kakao

import digdaserver.domain.oauth2.presentation.dto.res.oauth.OAuthUserResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader

@FeignClient(
    name = "KakaoOAuth2UserInfo",
    url = "https://kapi.kakao.com"
)
interface KakaoOAuth2UserFeignClient {

    @GetMapping("/v2/user/me")
    fun getUserInfo(
        @RequestHeader("Authorization") accessToken: String
    ): OAuthUserResponse
}
