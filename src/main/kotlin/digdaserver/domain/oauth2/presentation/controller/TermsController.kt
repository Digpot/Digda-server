package digdaserver.domain.oauth2.presentation.controller

import digdaserver.domain.oauth2.application.service.TermsService
import digdaserver.domain.oauth2.presentation.dto.req.TermsAgreeRequest
import digdaserver.domain.oauth2.presentation.dto.res.TermsAgreeResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "인증 API")
class TermsController(
    private val termsService: TermsService
) {

    @Operation(summary = "약관 동의", description = "신규 가입자가 필수/선택 약관에 동의합니다. isNewUser: true일 때만 호출.")
    @PostMapping("/terms")
    fun agreeToTerms(
        @AuthenticationPrincipal userId: String,
        @RequestBody request: TermsAgreeRequest
    ): ResponseEntity<TermsAgreeResponse> {
        val response = termsService.agreeToTerms(userId.toLong(), request)
        return ResponseEntity.ok(response)
    }
}
