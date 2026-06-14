package digdaserver.domain.deletionrequest.domain.repository

import digdaserver.domain.deletionrequest.domain.entity.DeletionRequest
import digdaserver.domain.deletionrequest.domain.entity.DeletionRequestStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface DeletionRequestRepository : JpaRepository<DeletionRequest, Long> {

    /** 어드민 목록 — 전체, 최신순 페이징. */
    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<DeletionRequest>

    /** 어드민 목록 — 상태 필터, 최신순 페이징. */
    fun findAllByStatusOrderByCreatedAtDesc(
        status: DeletionRequestStatus,
        pageable: Pageable
    ): Page<DeletionRequest>
}
