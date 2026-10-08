package digdaserver.global.common.masking

/**
 * 어드민 응답용 개인정보 마스킹 규칙.
 *
 * 화면에서만 가리면 네트워크 응답에 원문이 그대로 남는다. 그래서 서버가 마스킹한 값을
 * 내려주고, 원문은 관리자 비밀번호를 다시 확인하는 열람 API(`/api/admin/pii/reveal`)로만 준다.
 *
 * - 이름: 첫 글자·끝 글자만 남긴다. 2자는 끝 글자를 가린다 (홍길동 → 홍*동, 홍길 → 홍*, 남궁민수 → 남**수)
 * - 이메일: 아이디 앞 2자리만 남기고 도메인은 그대로 (chltm517@naver.com → ch******@naver.com)
 * - 전화번호: 가운데 자리를 가린다 (010-1234-5678 → 010-****-5678)
 * - 식별자(social_id 등): 앞 4자리만 남긴다
 * - 비밀번호·토큰: 마스킹이 아니라 통째로 가린다 — 열람 API 로도 주지 않는다
 */
object PiiMasker {

    const val MASK = '*'
    const val REDACTED = "[REDACTED]"

    fun name(raw: String?): String? {
        if (raw.isNullOrBlank()) return raw
        val s = raw.trim()
        val chars = s.codePoints().toArray()
        return when (chars.size) {
            1 -> MASK.toString()
            2 -> String(chars, 0, 1) + MASK
            else -> String(chars, 0, 1) + MASK.toString().repeat(chars.size - 2) + String(chars, chars.size - 1, 1)
        }
    }

    fun email(raw: String?): String? {
        if (raw.isNullOrBlank()) return raw
        val at = raw.lastIndexOf('@')
        if (at <= 0) return identifier(raw)
        val local = raw.substring(0, at)
        val domain = raw.substring(at)
        val keep = if (local.length <= 2) 1 else 2
        return local.take(keep) + MASK.toString().repeat((local.length - keep).coerceAtLeast(1)) + domain
    }

    fun phone(raw: String?): String? {
        if (raw.isNullOrBlank()) return raw
        val digits = raw.filter { it.isDigit() }
        if (digits.length < 9) return identifier(raw)
        val head = digits.take(3)
        val tail = digits.takeLast(4)
        return "$head-****-$tail"
    }

    fun identifier(raw: String?): String? {
        if (raw.isNullOrBlank()) return raw
        if (raw.length <= 4) return MASK.toString().repeat(raw.length)
        return raw.take(4) + MASK.toString().repeat(minOf(raw.length - 4, 12))
    }
}
