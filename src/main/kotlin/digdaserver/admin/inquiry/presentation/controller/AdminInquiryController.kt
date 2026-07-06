package digdaserver.admin.inquiry.presentation.controller

import digdaserver.admin.common.dto.res.AdminPageResponse
import digdaserver.admin.inquiry.application.service.AdminInquiryService
import digdaserver.admin.inquiry.presentation.dto.req.AnswerInquiryRequest
import digdaserver.admin.inquiry.presentation.dto.res.AdminInquiryResponse
import digdaserver.domain.inquiry.domain.entity.InquiryStatus
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

@RestController
@RequestMapping("/api/admin/inquiries")
@Tag(name = "Admin - Inquiry", description = "관리자 고객센터 문의 관리 API")
class AdminInquiryController(
    private val adminInquiryService: AdminInquiryService
) {

    @Operation(summary = "문의 목록 조회", description = "상태 필터 + 페이징. 최신순.")
    @GetMapping
    fun search(
        @RequestParam(required = false) status: InquiryStatus?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<AdminPageResponse<AdminInquiryResponse>> {
        return ResponseEntity.ok(adminInquiryService.search(status, page, size))
    }

    @Operation(summary = "문의 답변 등록", description = "답변 내용을 저장하고 ANSWERED(답변 완료)로 전이합니다.")
    @PatchMapping("/{inquiryId}/answer")
    fun answer(
        @PathVariable inquiryId: Long,
        @RequestBody @Valid
        request: AnswerInquiryRequest
    ): ResponseEntity<AdminInquiryResponse> {
        return ResponseEntity.ok(adminInquiryService.answer(inquiryId, request.answer))
    }
}
