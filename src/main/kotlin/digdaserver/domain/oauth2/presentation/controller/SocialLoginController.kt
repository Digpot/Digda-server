package digdaserver.domain.oauth2.presentation.controller

import digdaserver.domain.oauth2.application.service.SocialLoginService
import digdaserver.domain.oauth2.domain.entity.SocialProvider
import digdaserver.domain.oauth2.presentation.dto.req.LoginRequest
import digdaserver.domain.oauth2.presentation.dto.req.SocialTokenRequest
import digdaserver.domain.oauth2.presentation.dto.res.LoginResponse
import digdaserver.domain.oauth2.presentation.dto.res.LoginToken
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "Auth", description = "인증 API")
class SocialLoginController(
    private val socialLoginService: SocialLoginService
) {

    private val log = LoggerFactory.getLogger(SocialLoginController::class.java)

    @Operation(summary = "소셜 로그인", description = "소셜 프로바이더 토큰으로 로그인. 최초 로그인 시 계정 자동 생성.")
    @PostMapping("/auth/login")
    fun login(
        @Valid @RequestBody request: LoginRequest
    ): ResponseEntity<LoginResponse> {
        log.info("소셜 로그인 요청: provider={}", request.provider)
        val response = socialLoginService.login(request)
        return ResponseEntity.ok(response)
    }

    // ── 기존 호환 (테스트용) ──

    @Operation(summary = "[TEST] 소셜 로그인 URL 조회", description = "각 플랫폼별 소셜 로그인 URL을 반환합니다. (테스트용)")
    @GetMapping("/api/test/oauth2/login/{provider}")
    fun getLoginUrl(@PathVariable provider: SocialProvider): ResponseEntity<String> {
        log.info("소셜 로그인 URL 요청: {}", provider)
        val loginUrl = socialLoginService.getLoginUrl(provider)
        return ResponseEntity.ok(loginUrl)
    }

    @Operation(summary = "[TEST] 소셜 로그인 콜백", description = "테스트용 콜백 API")
    @GetMapping("/api/callback/{provider}")
    fun socialLogin(
        @PathVariable provider: SocialProvider,
        @RequestParam("code") code: String
    ): ResponseEntity<LoginToken> {
        log.info("소셜 로그인: provider={}, code={}", provider, code)
        val loginToken = socialLoginService.loginWithCode(provider, code)
        return ResponseEntity.ok(loginToken)
    }

    @Operation(summary = "[TEST] 소셜 로그인 콜백", description = "테스트용 애플 콜백 API")
    @PostMapping("/api/callback/apple")
    fun socialAppleLogin(
        @RequestParam("code") code: String
    ): ResponseEntity<LoginToken> {
        val loginToken = socialLoginService.loginWithCode(SocialProvider.APPLE, code)
        return ResponseEntity.ok(loginToken)
    }
}
