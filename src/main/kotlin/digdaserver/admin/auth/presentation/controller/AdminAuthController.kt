package digdaserver.admin.auth.presentation.controller

import digdaserver.admin.auth.application.service.AdminAuthService
import digdaserver.admin.auth.presentation.dto.req.AdminLoginRequest
import digdaserver.admin.auth.presentation.dto.res.AdminLoginResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/auth")
@Tag(name = "Admin - Auth", description = "관리자 인증 API")
class AdminAuthController(
    private val adminAuthService: AdminAuthService
) {

    @Operation(summary = "관리자 로그인", description = "이메일/비밀번호 기반 관리자 로그인. ADMIN 역할만 허용하며 JWT Access/Refresh 토큰을 발급합니다.")
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: AdminLoginRequest
    ): ResponseEntity<AdminLoginResponse> {
        return ResponseEntity.ok(adminAuthService.login(request))
    }
}
