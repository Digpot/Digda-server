package digdaserver.admin.inquiry.application.service.impl

import digdaserver.admin.common.dto.res.AdminPageResponse
import digdaserver.admin.inquiry.application.service.AdminInquiryService
import digdaserver.admin.inquiry.presentation.dto.res.AdminInquiryResponse
import digdaserver.domain.inquiry.domain.entity.InquiryStatus
import digdaserver.domain.inquiry.domain.repository.InquiryRepository
import digdaserver.domain.notification.application.service.NotificationService
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminInquiryServiceImpl(
    private val inquiryRepository: InquiryRepository,
    private val notificationService: NotificationService
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
    override fun answer(inquiryId: Long, answer: String): AdminInquiryResponse {
        val inquiry = inquiryRepository.findById(inquiryId)
            .orElseThrow { DigdaException(ErrorCode.RESOURCE_NOT_FOUND) }
        inquiry.answer(answer.trim())

        // 답변 수정 시에도 다시 알린다 — 내용이 바뀐 걸 문의자가 알아야 하므로.
        notificationService.notifyInquiryAnswered(inquiry.user.id, inquiry.id)

        return AdminInquiryResponse.from(inquiry)
    }
}
