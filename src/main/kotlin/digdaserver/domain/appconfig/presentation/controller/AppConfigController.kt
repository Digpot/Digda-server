package digdaserver.domain.appconfig.presentation.controller

import digdaserver.domain.appconfig.application.service.AppConfigService
import digdaserver.domain.appconfig.presentation.dto.res.AppConfigResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 앱이 전역 운영 설정(대공지·피드백)을 조회한다. */
@RestController
@RequestMapping("/app-config")
@Tag(name = "App Config", description = "앱 전역 운영 설정 조회(대공지·피드백)")
class AppConfigController(
    private val appConfigService: AppConfigService
) {

    @Operation(summary = "앱 운영 설정 조회", description = "대공지 노출/메시지 + 피드백 노출/URL")
    @GetMapping
    fun get(
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<AppConfigResponse> = ResponseEntity.ok(appConfigService.get())
}
