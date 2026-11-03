package digdaserver.admin.pii.presentation.controller

import digdaserver.admin.pii.application.service.AdminPiiService
import digdaserver.admin.pii.presentation.dto.req.RevealPiiRequest
import digdaserver.admin.pii.presentation.dto.res.RevealPiiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * 어드민 응답의 개인정보는 기본이 마스킹이다. 원문은 여기서만, 관리자 비밀번호를 다시 확인한 뒤 준다.
 * 5회 틀리면 10분 잠기고, 성공한 열람은 전부 user_action_log(PII_REVEAL) 에 남는다.
 */
@RestController
@RequestMapping("/api/admin/pii")
@Tag(name = "Admin - PII", description = "개인정보 원문 열람(비밀번호 재확인)")
class AdminPiiController(
    private val adminPiiService: AdminPiiService
) {

    @Operation(summary = "개인정보 원문 열람", description = "관리자 비밀번호 재확인 후 마스킹 전 원문을 돌려준다. 열람 이력이 남는다.")
    @PostMapping("/reveal")
    fun reveal(
        @AuthenticationPrincipal userId: String,
        @Valid
        @RequestBody
        request: RevealPiiRequest
    ): ResponseEntity<RevealPiiResponse> =
        ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body(adminPiiService.reveal(UUID.fromString(userId), request))
}
