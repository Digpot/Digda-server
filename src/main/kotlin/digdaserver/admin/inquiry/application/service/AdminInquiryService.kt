package digdaserver.admin.inquiry.application.service

import digdaserver.admin.common.dto.res.AdminPageResponse
import digdaserver.admin.inquiry.presentation.dto.res.AdminInquiryResponse
import digdaserver.domain.inquiry.domain.entity.InquiryStatus

interface AdminInquiryService {

    /** 문의 목록 — 상태 필터(없으면 전체), 최신순 페이징. */
    fun search(status: InquiryStatus?, page: Int, size: Int): AdminPageResponse<AdminInquiryResponse>

    /** 문의에 답변 등록 — 답변 내용 저장 + ANSWERED 전이. */
    fun answer(inquiryId: Long, answer: String): AdminInquiryResponse
}
