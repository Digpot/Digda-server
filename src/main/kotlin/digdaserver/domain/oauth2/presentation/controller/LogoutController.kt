package digdaserver.domain.oauth2.presentation.controller

import digdaserver.domain.oauth2.application.service.LogoutService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "인증 API")
class LogoutController(
    private val logoutService: LogoutService
) {

    @Operation(summary = "로그아웃", description = "서버 측 리프레시 토큰 무효화 + 디바이스 토큰 해제.")
    @PostMapping("/logout")
    fun logout(
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<Void> {
        logoutService.logout(userId)
        return ResponseEntity.noContent().build()
    }
}
