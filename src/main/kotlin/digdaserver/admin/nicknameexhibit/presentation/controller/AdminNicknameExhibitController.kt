package digdaserver.admin.nicknameexhibit.presentation.controller

import digdaserver.admin.common.dto.res.AdminPageResponse
import digdaserver.admin.nicknameexhibit.application.service.AdminNicknameExhibitService
import digdaserver.admin.nicknameexhibit.presentation.dto.req.AddExhibitAccessRequest
import digdaserver.admin.nicknameexhibit.presentation.dto.req.CreateNicknameExhibitRequest
import digdaserver.admin.nicknameexhibit.presentation.dto.req.UpdateNicknameExhibitRequest
import digdaserver.admin.nicknameexhibit.presentation.dto.res.AdminExhibitAccessResponse
import digdaserver.admin.nicknameexhibit.presentation.dto.res.AdminNicknameExhibitResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * 어드민 디그다 역대 별명 전시관 관리.
 * - 별명 카드(콘텐츠) CRUD
 * - 접근 허용 사용자 등록/조회/해제
 *
 * `/api/admin/**` 는 SecurityConfig 에서 ROLE_ADMIN 으로 보호된다.
 */
@RestController
@RequestMapping("/api/admin/nickname-exhibits")
@Tag(name = "Admin - Nickname Exhibit", description = "관리자 역대 별명 전시관 관리 API")
class AdminNicknameExhibitController(
    private val adminNicknameExhibitService: AdminNicknameExhibitService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    // ── 콘텐츠(별명 카드) CRUD ──

    @Operation(summary = "별명 카드 목록 조회", description = "별명 키워드 검색 + 정렬순 페이지네이션")
    @GetMapping
    fun search(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<AdminPageResponse<AdminNicknameExhibitResponse>> {
        log.info("api=GET /api/admin/nickname-exhibits, keyword={}, page={}", keyword, page)
        return ResponseEntity.ok(adminNicknameExhibitService.search(keyword, page, size))
    }

    @Operation(summary = "별명 카드 등록")
    @PostMapping
    fun create(
        @Valid @RequestBody request: CreateNicknameExhibitRequest
    ): ResponseEntity<AdminNicknameExhibitResponse> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(adminNicknameExhibitService.create(request))
    }

    @Operation(summary = "별명 카드 수정", description = "전송된 필드만 부분 수정")
    @PatchMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateNicknameExhibitRequest
    ): ResponseEntity<AdminNicknameExhibitResponse> {
        return ResponseEntity.ok(adminNicknameExhibitService.update(id, request))
    }

    @Operation(summary = "별명 카드 삭제")
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        adminNicknameExhibitService.delete(id)
        return ResponseEntity.noContent().build()
    }

    // ── 접근 허용 사용자 관리 ──

    @Operation(summary = "접근 허용 사용자 목록", description = "이름/이메일 키워드 검색 + 페이지네이션")
    @GetMapping("/access")
    fun searchAccess(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<AdminPageResponse<AdminExhibitAccessResponse>> {
        return ResponseEntity.ok(adminNicknameExhibitService.searchAccess(keyword, page, size))
    }

    @Operation(summary = "접근 허용 추가", description = "이미 허용된 사용자면 멱등 처리")
    @PostMapping("/access")
    fun addAccess(
        @Valid @RequestBody request: AddExhibitAccessRequest
    ): ResponseEntity<AdminExhibitAccessResponse> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(adminNicknameExhibitService.addAccess(request.userId))
    }

    @Operation(summary = "접근 허용 해제")
    @DeleteMapping("/access/{userId}")
    fun removeAccess(@PathVariable userId: UUID): ResponseEntity<Void> {
        adminNicknameExhibitService.removeAccess(userId)
        return ResponseEntity.noContent().build()
    }
}
