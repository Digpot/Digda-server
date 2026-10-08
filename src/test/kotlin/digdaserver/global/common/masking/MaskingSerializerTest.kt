package digdaserver.global.common.masking

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import digdaserver.admin.deletionrequest.presentation.dto.res.AdminDeletionRequestResponse
import digdaserver.admin.user.presentation.dto.res.AdminUserResponse
import digdaserver.domain.deletionrequest.domain.entity.DeletionRequestStatus
import digdaserver.domain.deletionrequest.domain.entity.DeletionRequestType
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/** 어드민 DTO 가 JSON 으로 나갈 때 원문이 새지 않는지 — 응답 본문 기준으로 본다. */
class MaskingSerializerTest {

    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    private val now = LocalDateTime.of(2026, 10, 8, 12, 0)

    @Test
    fun `사용자 응답의 이름과 이메일은 마스킹되어 나간다`() {
        val json = mapper.writeValueAsString(
            AdminUserResponse(
                userId = "u1",
                email = "chltm517@naver.com",
                name = "홍길동",
                profileImage = null,
                socialProvider = "KAKAO",
                role = "USER",
                restricted = false,
                createdAt = now,
                updatedAt = now
            )
        )
        assertTrue(json.contains("\"ch******@naver.com\""), json)
        assertTrue(json.contains("\"홍*동\""), json)
        assertFalse(json.contains("chltm517"), json)
        assertFalse(json.contains("홍길동"), json)
    }

    @Test
    fun `null 이메일은 null 그대로`() {
        val json = mapper.writeValueAsString(
            AdminUserResponse("u1", null, "홍길동", null, "APPLE", "USER", false, now, now)
        )
        assertTrue(json.contains("\"email\":null"), json)
    }

    @Test
    fun `삭제 요청 이메일도 마스킹`() {
        val json = mapper.writeValueAsString(
            AdminDeletionRequestResponse(
                1L,
                DeletionRequestType.ACCOUNT,
                "someone@gmail.com",
                null,
                null,
                DeletionRequestStatus.PENDING,
                now,
                null
            )
        )
        assertFalse(json.contains("someone@"), json)
    }
}
