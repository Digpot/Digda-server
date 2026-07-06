package digdaserver.domain.inquiry.application.service

import digdaserver.domain.inquiry.presentation.dto.req.CreateInquiryRequest
import digdaserver.domain.inquiry.presentation.dto.res.InquiryResponse
import java.util.UUID

interface InquiryService {

    /** 고객센터 문의 작성. 하루 2건 초과 시 INQUIRY_DAILY_LIMIT. */
    fun create(userId: UUID, request: CreateInquiryRequest): InquiryResponse

    /** 내 문의 목록(최신순). */
    fun myInquiries(userId: UUID): List<InquiryResponse>
}
