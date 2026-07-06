package digdaserver.admin.title.presentation.controller

import digdaserver.admin.title.application.service.AdminTitleService
import digdaserver.admin.title.presentation.dto.req.GrantTitleRequest
import digdaserver.admin.title.presentation.dto.res.AdminUserTitleResponse
import digdaserver.domain.title.presentation.dto.res.TitleCatalogResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * 어드민 칭호 관리 — 카탈로그 조회 + 특정 사용자에게 칭호 부여/회수.
 * `/api/admin` 하위라 SecurityConfig 에서 ROLE_ADMIN 으로 보호된다.
 */
@RestController
@RequestMapping("/api/admin/titles")
@Tag(name = "Admin - Title", description = "관리자 칭호 부여/회수 API")
class AdminTitleController(
    private val adminTitleService: AdminTitleService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Operation(summary = "칭호 카탈로그 조회", description = "부여 가능한 전체 칭호 목록")
    @GetMapping("/catalog")
    fun catalog(): ResponseEntity<List<TitleCatalogResponse>> =
        ResponseEntity.ok(adminTitleService.catalog())

    @Operation(summary = "사용자 보유 칭호 조회")
    @GetMapping("/users/{userId}")
    fun userTitles(@PathVariable userId: UUID): ResponseEntity<List<AdminUserTitleResponse>> =
        ResponseEntity.ok(adminTitleService.userTitles(userId))

    @Operation(summary = "칭호 부여", description = "대상 사용자에게 칭호 지급(멱등). 부여 후 보유 목록 반환.")
    @PostMapping("/grant")
    fun grant(
        @Valid @RequestBody
        request: GrantTitleRequest
    ): ResponseEntity<List<AdminUserTitleResponse>> {
        log.info("api=POST /api/admin/titles/grant, userId={}, code={}", request.userId, request.code)
        return ResponseEntity.ok(adminTitleService.grant(request.userId, request.code))
    }

    @Operation(summary = "칭호 회수")
    @DeleteMapping("/users/{userId}/{code}")
    fun revoke(
        @PathVariable userId: UUID,
        @PathVariable code: String
    ): ResponseEntity<List<AdminUserTitleResponse>> {
        log.info("api=DELETE /api/admin/titles/users/{}/{}", userId, code)
        return ResponseEntity.ok(adminTitleService.revoke(userId, code))
    }
}
