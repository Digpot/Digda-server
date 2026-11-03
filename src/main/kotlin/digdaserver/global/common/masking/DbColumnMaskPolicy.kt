package digdaserver.global.common.masking

/**
 * DB 테이블 조회(어드민 DB 뷰어)에서 컬럼별로 어떻게 가릴지 정한다.
 *
 * 컬럼 이름만으로 판단한다 — 테이블이 늘어도 `email`, `*_token` 같은 이름이면 자동으로 걸린다.
 * `name` 은 그룹방·상점 아이템에도 있는 흔한 이름이라 사람 이름이 들어가는 테이블에서만 가린다.
 */
enum class DbMaskType { NONE, NAME, EMAIL, PHONE, IDENTIFIER, SECRET }

object DbColumnMaskPolicy {

    private val SECRET = Regex("(^|_)(password|passwd|secret|token|access_token|refresh_token|fcm_token|api_key)$")
    private val EMAIL = Regex("(^|_)email$")
    private val PHONE = Regex("(^|_)(phone|phone_number|mobile|tel)$")
    private val IDENTIFIER = Regex("^(social_id|provider_id|ip|ip_address|client_ip)$")
    private val PERSON_NAME = setOf("real_name", "user_name", "author_name", "reporter_name")
    private val GENERIC_NAME = setOf("name", "display_name")

    /** `name`/`display_name` 이 사람 이름인 테이블. 그 밖(group_room, shop_item …)은 사물 이름이다. */
    private val PERSON_TABLES = setOf("user")

    fun typeOf(table: String, column: String): DbMaskType {
        val c = column.lowercase()
        return when {
            SECRET.containsMatchIn(c) -> DbMaskType.SECRET
            EMAIL.containsMatchIn(c) -> DbMaskType.EMAIL
            PHONE.containsMatchIn(c) -> DbMaskType.PHONE
            IDENTIFIER.matches(c) -> DbMaskType.IDENTIFIER
            c in PERSON_NAME -> DbMaskType.NAME
            c in GENERIC_NAME && table in PERSON_TABLES -> DbMaskType.NAME
            else -> DbMaskType.NONE
        }
    }

    /** 목록 조회용 — 개인정보는 마스킹, 비밀값은 통째로 가린다. */
    fun mask(type: DbMaskType, value: Any?): Any? {
        if (value == null || type == DbMaskType.NONE) return value
        val s = value.toString()
        return when (type) {
            DbMaskType.NAME -> PiiMasker.name(s)
            DbMaskType.EMAIL -> PiiMasker.email(s)
            DbMaskType.PHONE -> PiiMasker.phone(s)
            DbMaskType.IDENTIFIER -> PiiMasker.identifier(s)
            DbMaskType.SECRET -> PiiMasker.REDACTED
            DbMaskType.NONE -> value
        }
    }

    /** 원문 열람용 — 개인정보는 풀지만 비밀번호·토큰은 열람 API 로도 내주지 않는다. */
    fun reveal(type: DbMaskType, value: Any?): Any? =
        if (value != null && type == DbMaskType.SECRET) PiiMasker.REDACTED else value
}
