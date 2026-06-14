package digdaserver.admin.deletionrequest.presentation.controller

import digdaserver.admin.common.dto.res.AdminPageResponse
import digdaserver.admin.deletionrequest.application.service.AdminDeletionRequestService
import digdaserver.admin.deletionrequest.presentation.dto.res.AdminDeletionRequestResponse
import digdaserver.domain.deletionrequest.domain.entity.DeletionRequestStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/deletion-requests")
@Tag(name = "Admin - DeletionRequest", description = "관리자 계정/데이터 삭제 요청 관리 API")
class AdminDeletionRequestController(
    private val adminDeletionRequestService: AdminDeletionRequestService
) {

    @Operation(summary = "삭제 요청 목록 조회", description = "상태 필터 + 페이징. 최신순.")
    @GetMapping
    fun search(
        @RequestParam(required = false) status: DeletionRequestStatus?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<AdminPageResponse<AdminDeletionRequestResponse>> {
        return ResponseEntity.ok(adminDeletionRequestService.search(status, page, size))
    }

    @Operation(summary = "삭제 요청 처리 완료", description = "요청을 DONE(처리 완료)으로 전이합니다.")
    @PatchMapping("/{deletionRequestId}/done")
    fun markDone(
        @PathVariable deletionRequestId: Long
    ): ResponseEntity<AdminDeletionRequestResponse> {
        return ResponseEntity.ok(adminDeletionRequestService.markDone(deletionRequestId))
    }
}
