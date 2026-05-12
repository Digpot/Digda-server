package digdaserver.admin.user.presentation.controller

import digdaserver.admin.common.dto.res.AdminPageResponse
import digdaserver.admin.user.application.service.AdminUserService
import digdaserver.admin.user.presentation.dto.req.AdminUpdateUserRoleRequest
import digdaserver.admin.user.presentation.dto.res.AdminUserResponse
import digdaserver.domain.user.domain.entity.Role
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "Admin - User", description = "관리자 사용자 관리 API")
class AdminUserController(
    private val adminUserService: AdminUserService
) {

    @Operation(summary = "사용자 목록 조회", description = "페이징, 키워드(이름/이메일), 권한 필터를 지원합니다.")
    @GetMapping
    fun search(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) role: Role?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<AdminPageResponse<AdminUserResponse>> {
        return ResponseEntity.ok(adminUserService.search(keyword, role, page, size))
    }

    @Operation(summary = "사용자 상세 조회")
    @GetMapping("/{userId}")
    fun getDetail(@PathVariable userId: UUID): ResponseEntity<AdminUserResponse> {
        return ResponseEntity.ok(adminUserService.getDetail(userId))
    }

    @Operation(summary = "사용자 권한 변경")
    @PatchMapping("/{userId}/role")
    fun updateRole(
        @PathVariable userId: UUID,
        @Valid
        @RequestBody
        request: AdminUpdateUserRoleRequest
    ): ResponseEntity<AdminUserResponse> {
        return ResponseEntity.ok(adminUserService.updateRole(userId, request.role))
    }
}
