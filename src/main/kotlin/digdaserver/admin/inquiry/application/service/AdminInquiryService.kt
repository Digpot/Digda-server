package digdaserver.admin.inquiry.application.service

import digdaserver.admin.common.dto.res.AdminPageResponse
import digdaserver.admin.inquiry.presentation.dto.res.AdminInquiryResponse
import digdaserver.domain.inquiry.domain.entity.InquiryStatus

interface AdminInquiryService {

    /** 문의 목록 — 상태 필터(없으면 전체), 최신순 페이징. */
    fun search(status: InquiryStatus?, page: Int, size: Int): AdminPageResponse<AdminInquiryResponse>

    /** 문의를 답변 완료(ANSWERED)로 전이. */
    fun markAnswered(inquiryId: Long): AdminInquiryResponse
}
