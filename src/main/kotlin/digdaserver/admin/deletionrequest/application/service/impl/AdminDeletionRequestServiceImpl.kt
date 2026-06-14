package digdaserver.admin.deletionrequest.application.service.impl

import digdaserver.admin.common.dto.res.AdminPageResponse
import digdaserver.admin.deletionrequest.application.service.AdminDeletionRequestService
import digdaserver.admin.deletionrequest.presentation.dto.res.AdminDeletionRequestResponse
import digdaserver.domain.deletionrequest.domain.entity.DeletionRequestStatus
import digdaserver.domain.deletionrequest.domain.repository.DeletionRequestRepository
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminDeletionRequestServiceImpl(
    private val deletionRequestRepository: DeletionRequestRepository
) : AdminDeletionRequestService {

    override fun search(
        status: DeletionRequestStatus?,
        page: Int,
        size: Int
    ): AdminPageResponse<AdminDeletionRequestResponse> {
        val pageable = PageRequest.of(page, size)
        val result = if (status != null) {
            deletionRequestRepository.findAllByStatusOrderByCreatedAtDesc(status, pageable)
        } else {
            deletionRequestRepository.findAllByOrderByCreatedAtDesc(pageable)
        }
        return AdminPageResponse.of(result) { AdminDeletionRequestResponse.from(it) }
    }

    @Transactional
    override fun markDone(deletionRequestId: Long): AdminDeletionRequestResponse {
        val entity = deletionRequestRepository.findById(deletionRequestId)
            .orElseThrow { DigdaException(ErrorCode.RESOURCE_NOT_FOUND) }
        entity.markDone()
        return AdminDeletionRequestResponse.from(entity)
    }
}
