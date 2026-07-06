package digdaserver.domain.oauth2.presentation.dto.res.oauth

import com.fasterxml.jackson.annotation.JsonProperty

data class OAuthUserResponse(

    @JsonProperty("id")
    val id: String? = null,

    @JsonProperty("kakao_account")
    val account: OAuthAccount? = null
) {

    fun getName(): String? {
        return account?.profile?.nickname
    }

    fun getEmail(): String? {
        return account?.email
    }

    fun getProfile(): String? {
        return account?.profile?.profileImageUrl
    }

    data class OAuthAccount(
        @JsonProperty("profile")
        val profile: Profile? = null,

        @JsonProperty("email")
        val email: String? = null
    )

    data class Profile(
        @JsonProperty("nickname")
        val nickname: String? = null,

        @JsonProperty("profile_image_url")
        val profileImageUrl: String? = null
    )
}
