package digdaserver.domain.inquiry.domain.repository

import digdaserver.domain.inquiry.domain.entity.Inquiry
import digdaserver.domain.inquiry.domain.entity.InquiryStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.UUID

interface InquiryRepository : JpaRepository<Inquiry, Long> {

    /** 하루 작성 한도 검사용 — 특정 사용자가 [from] 이후 작성한 문의 수. */
    fun countByUserIdAndCreatedAtAfter(userId: UUID, from: LocalDateTime): Long

    /** 내 문의 목록(최신순). */
    fun findAllByUserIdOrderByCreatedAtDesc(userId: UUID): List<Inquiry>

    /** 어드민 목록 — 상태 필터(없으면 전체), 최신순 페이징. */
    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<Inquiry>

    fun findAllByStatusOrderByCreatedAtDesc(status: InquiryStatus, pageable: Pageable): Page<Inquiry>
}
