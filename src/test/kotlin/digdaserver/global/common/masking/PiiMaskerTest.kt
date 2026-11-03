package digdaserver.global.common.masking

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PiiMaskerTest {

    @Test
    fun `이름은 첫 글자와 끝 글자만 남긴다`() {
        assertEquals("*", PiiMasker.name("김"))
        assertEquals("홍*", PiiMasker.name("홍길"))
        assertEquals("홍*동", PiiMasker.name("홍길동"))
        assertEquals("남**수", PiiMasker.name("남궁민수"))
        assertEquals("J********h", PiiMasker.name("John Smith"))
        assertNull(PiiMasker.name(null))
    }

    @Test
    fun `이메일은 아이디 앞 2자리와 도메인만 남긴다`() {
        assertEquals("ch******@naver.com", PiiMasker.email("chltm517@naver.com"))
        assertEquals("a*@b.com", PiiMasker.email("ab@b.com"))
        assertEquals("a*@b.com", PiiMasker.email("a@b.com"))
        assertNull(PiiMasker.email(null))
    }

    @Test
    fun `전화번호는 가운데를 가린다`() {
        assertEquals("010-****-5678", PiiMasker.phone("010-1234-5678"))
        assertEquals("010-****-5678", PiiMasker.phone("01012345678"))
    }

    @Test
    fun `DB 컬럼 정책 — 비밀값은 통째로, 사람 이름은 사람 테이블에서만`() {
        assertEquals(DbMaskType.SECRET, DbColumnMaskPolicy.typeOf("admin_credential", "password"))
        assertEquals(DbMaskType.SECRET, DbColumnMaskPolicy.typeOf("device", "token"))
        assertEquals(DbMaskType.EMAIL, DbColumnMaskPolicy.typeOf("deletion_request", "email"))
        assertEquals(DbMaskType.NAME, DbColumnMaskPolicy.typeOf("user", "name"))
        assertEquals(DbMaskType.NAME, DbColumnMaskPolicy.typeOf("user", "display_name"))
        assertEquals(DbMaskType.NONE, DbColumnMaskPolicy.typeOf("group_room", "name"))
        assertEquals(DbMaskType.NONE, DbColumnMaskPolicy.typeOf("shop_item", "display_name"))
        assertEquals(DbMaskType.IDENTIFIER, DbColumnMaskPolicy.typeOf("user", "social_id"))

        assertEquals(PiiMasker.REDACTED, DbColumnMaskPolicy.mask(DbMaskType.SECRET, "\$2a\$10\$abc"))
        assertEquals(PiiMasker.REDACTED, DbColumnMaskPolicy.reveal(DbMaskType.SECRET, "\$2a\$10\$abc"))
        assertEquals("홍길동", DbColumnMaskPolicy.reveal(DbMaskType.NAME, "홍길동"))
    }
}
