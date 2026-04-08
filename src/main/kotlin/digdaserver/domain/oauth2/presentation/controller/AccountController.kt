package digdaserver.domain.oauth2.presentation.controller

import digdaserver.domain.oauth2.application.service.AccountService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "인증 API")
class AccountController(
    private val accountService: AccountService
) {

    @Operation(summary = "회원 탈퇴", description = "계정 영구 삭제. 소유 중인 그룹이 있으면 먼저 양도 필요.")
    @DeleteMapping("/account")
    fun deleteAccount(
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<Void> {
        accountService.deleteAccount(UUID.fromString(userId))
        return ResponseEntity.ok().build()
    }
}
