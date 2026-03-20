package digdaserver.domain.user.domain.entity

enum class Role(val key: String) {

    USER("ROLE_USER"),
    ADMIN("ROLE_ADMIN");

    companion object {

        fun from(key: String): Role {
            return entries.find { it.key.equals(key, ignoreCase = true) }
                ?: throw IllegalArgumentException("유효하지 않은 역할입니다: $key")
        }

        fun getByValue(value: String): Role {
            return entries.find { it.key == value }
                ?: throw IllegalArgumentException("유효하지 않은 역할입니다: $value")
        }
    }
}
