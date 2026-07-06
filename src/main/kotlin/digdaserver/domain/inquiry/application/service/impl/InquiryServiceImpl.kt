package digdaserver.domain.inquiry.application.service.impl

import digdaserver.domain.inquiry.application.service.InquiryService
import digdaserver.domain.inquiry.domain.entity.Inquiry
import digdaserver.domain.inquiry.domain.repository.InquiryRepository
import digdaserver.domain.inquiry.presentation.dto.req.CreateInquiryRequest
import digdaserver.domain.inquiry.presentation.dto.res.InquiryResponse
import digdaserver.domain.user.domain.repository.UserRepository
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
@Transactional(readOnly = true)
class InquiryServiceImpl(
    private val inquiryRepository: InquiryRepository,
    private val userRepository: UserRepository
) : InquiryService {

    private val log = LoggerFactory.getLogger(javaClass)

    /** 하루 작성 한도. */
    private val dailyLimit = 2L

    @Transactional
    override fun create(userId: UUID, request: CreateInquiryRequest): InquiryResponse {
        val content = request.content.trim()
        if (content.isBlank()) throw DigdaException(ErrorCode.INQUIRY_CONTENT_REQUIRED)

        val user = userRepository.findById(userId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }

        // 오늘(00:00 이후) 작성 건수로 하루 2건 제한. 무분별한 도배 방지.
        val startOfToday = LocalDate.now().atStartOfDay()
        val todayCount = inquiryRepository.countByUserIdAndCreatedAtAfter(userId, startOfToday)
        if (todayCount >= dailyLimit) {
            log.info(
                "action=고객센터 문의 한도 초과, userId={}, todayCount={}",
                userId,
                todayCount
            )
            throw DigdaException(ErrorCode.INQUIRY_DAILY_LIMIT)
        }

        val saved = inquiryRepository.save(Inquiry(user = user, content = content))
        log.info(
            "action=고객센터 문의 접수, userId={}, inquiryId={}, todayCount={}",
            userId,
            saved.id,
            todayCount + 1
        )
        return InquiryResponse.from(saved)
    }

    override fun myInquiries(userId: UUID): List<InquiryResponse> {
        return inquiryRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
            .map { InquiryResponse.from(it) }
    }
}
