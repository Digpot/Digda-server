package digdaserver.admin.inquiry.application.service.impl

import digdaserver.admin.common.dto.res.AdminPageResponse
import digdaserver.admin.inquiry.application.service.AdminInquiryService
import digdaserver.admin.inquiry.presentation.dto.res.AdminInquiryResponse
import digdaserver.domain.inquiry.domain.entity.InquiryStatus
import digdaserver.domain.inquiry.domain.repository.InquiryRepository
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminInquiryServiceImpl(
    private val inquiryRepository: InquiryRepository
) : AdminInquiryService {

    override fun search(
        status: InquiryStatus?,
        page: Int,
        size: Int
    ): AdminPageResponse<AdminInquiryResponse> {
        val pageable = PageRequest.of(page, size)
        val result = if (status != null) {
            inquiryRepository.findAllByStatusOrderByCreatedAtDesc(status, pageable)
        } else {
            inquiryRepository.findAllByOrderByCreatedAtDesc(pageable)
        }
        return AdminPageResponse.of(result) { AdminInquiryResponse.from(it) }
    }

    @Transactional
    override fun markAnswered(inquiryId: Long): AdminInquiryResponse {
        val inquiry = inquiryRepository.findById(inquiryId)
            .orElseThrow { DigdaException(ErrorCode.RESOURCE_NOT_FOUND) }
        inquiry.markAnswered()
        return AdminInquiryResponse.from(inquiry)
    }
}
