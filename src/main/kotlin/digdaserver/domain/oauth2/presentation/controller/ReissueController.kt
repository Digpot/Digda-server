package digdaserver.domain.oauth2.presentation.controller

import digdaserver.domain.oauth2.application.service.ReissueService
import digdaserver.domain.oauth2.presentation.dto.req.RefreshRequest
import digdaserver.domain.oauth2.presentation.dto.res.LoginToken
import digdaserver.domain.oauth2.presentation.dto.res.RefreshResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "Auth", description = "인증 API")
class ReissueController(
    private val reissueService: ReissueService
) {

    @Operation(summary = "토큰 갱신", description = "Refresh Token으로 새로운 Access/Refresh Token을 발급합니다. (Rotation 적용)")
    @PostMapping("/auth/refresh")
    fun refresh(
        @Valid
        @RequestBody
        request: RefreshRequest
    ): ResponseEntity<RefreshResponse> {
        val loginToken = reissueService.reissue(request.refreshToken)
        return ResponseEntity.ok(
            RefreshResponse(
                accessToken = loginToken.accessToken,
                refreshToken = loginToken.refreshToken
            )
        )
    }

    @Operation(summary = "[기존 호환] 토큰 재발급", description = "기존 호환용 토큰 재발급 API")
    @PostMapping("/api/app/reissue")
    fun reissueApp(@RequestBody request: RefreshRequest): ResponseEntity<LoginToken> {
        val loginToken = reissueService.reissue(request.refreshToken)
        return ResponseEntity.ok(loginToken)
    }
}
