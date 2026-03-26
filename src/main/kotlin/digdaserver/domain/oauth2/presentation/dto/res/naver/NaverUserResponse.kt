package digdaserver.domain.oauth2.presentation.dto.res.naver

import com.fasterxml.jackson.annotation.JsonProperty
import digdaserver.domain.oauth2.presentation.dto.res.oauth.OAuthUserResponse

data class NaverUserResponse(

    @JsonProperty("resultcode")
    val resultCode: String? = null,

    @JsonProperty("message")
    val message: String? = null,

    @JsonProperty("response")
    val response: UserInfo? = null
) {

    data class UserInfo(
        @JsonProperty("id")
        val id: String? = null,

        @JsonProperty("name")
        val name: String? = null,

        @JsonProperty("email")
        val email: String? = null,

        @JsonProperty("profile_image")
        val profileImage: String? = null
    )

    /** OAuth2UserResponse로 변환 */
    fun toOAuth2UserResponse(): OAuthUserResponse? {
        val r = response ?: return null

        return OAuthUserResponse(
            r.id,
            OAuthUserResponse.OAuthAccount(
                OAuthUserResponse.Profile(
                    r.name,
                    r.profileImage
                ),
                r.email
            )
        )
    }
}
