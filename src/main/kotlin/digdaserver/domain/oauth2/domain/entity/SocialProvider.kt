package digdaserver.domain.oauth2.domain.entity

enum class SocialProvider(val value: String) {
    KAKAO("kakao"),
    NAVER("naver"),
    APPLE("apple"),
    ADMIN("admin");

    companion object {
        fun from(value: String): SocialProvider {
            return entries.find { it.value.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("지원하지 않는 소셜 로그인 제공자입니다: $value")
        }
    }
}
