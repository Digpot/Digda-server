package digdaserver.domain.oauth2.presentation.dto.res.apple

import com.fasterxml.jackson.annotation.JsonProperty

data class AppleTokenResponse(

    @JsonProperty("access_token")
    val accessToken: String? = null,

    @JsonProperty("expires_in")
    val expiresIn: Long? = null,

    @JsonProperty("id_token")
    val idToken: String? = null,

    @JsonProperty("refresh_token")
    val refreshToken: String? = null,

    @JsonProperty("token_type")
    val tokenType: String? = null,

    @JsonProperty("error")
    val error: String? = null,

    @JsonProperty("error_description")
    val errorDescription: String? = null
) {

    /** Apple의 경우 유저 정보는 ID Token에 포함되어 있음 */
    fun getUserInfoToken(): String? {
        return idToken ?: accessToken
    }

    /** 에러 존재 여부 */
    fun hasError(): Boolean {
        return !error.isNullOrBlank()
    }

    /** 에러 메시지 반환 */
    fun getErrorMessage(): String? {
        if (!hasError()) return null
        return if (!errorDescription.isNullOrBlank()) "$error: $errorDescription" else error
    }
}
