package digdaserver.domain.oauth2.presentation.dto.res.naver

import com.fasterxml.jackson.annotation.JsonProperty

data class NaverTokenResponse(

    @JsonProperty("access_token")
    val accessToken: String? = null,

    @JsonProperty("refresh_token")
    val refreshToken: String? = null,

    @JsonProperty("expires_in")
    val expiresIn: Long? = null,

    @JsonProperty("token_type")
    val tokenType: String? = null
)
