package digdaserver.admin.appconfig.presentation.controller

import digdaserver.domain.appconfig.application.service.AppConfigService
import digdaserver.domain.appconfig.presentation.dto.req.UpdateAppConfigRequest
import digdaserver.domain.appconfig.presentation.dto.res.AppConfigResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 어드민 앱 운영 설정 — 대공지(전광판) 노출/메시지, 피드백 노출/URL 관리.
 * `/api/admin` 하위라 SecurityConfig 에서 ROLE_ADMIN 으로 보호된다.
 */
@RestController
@RequestMapping("/api/admin/app-config")
@Tag(name = "Admin - App Config", description = "관리자 앱 운영 설정(대공지·피드백)")
class AdminAppConfigController(
    private val appConfigService: AppConfigService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Operation(summary = "앱 운영 설정 조회")
    @GetMapping
    fun get(): ResponseEntity<AppConfigResponse> =
        ResponseEntity.ok(appConfigService.get())

    @Operation(summary = "앱 운영 설정 수정", description = "대공지 노출/메시지 + 피드백 노출/URL 저장")
    @PutMapping
    fun update(
        @RequestBody request: UpdateAppConfigRequest
    ): ResponseEntity<AppConfigResponse> {
        log.info(
            "api=PUT /api/admin/app-config, notice={}, feedback={}, maintenance={}",
            request.noticeEnabled,
            request.feedbackEnabled,
            request.maintenanceEnabled
        )
        return ResponseEntity.ok(appConfigService.update(request))
    }
}
