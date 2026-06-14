package digdaserver.admin.deletionrequest.application.service

import digdaserver.admin.common.dto.res.AdminPageResponse
import digdaserver.admin.deletionrequest.presentation.dto.res.AdminDeletionRequestResponse
import digdaserver.domain.deletionrequest.domain.entity.DeletionRequestStatus

interface AdminDeletionRequestService {

    fun search(
        status: DeletionRequestStatus?,
        page: Int,
        size: Int
    ): AdminPageResponse<AdminDeletionRequestResponse>

    fun markDone(deletionRequestId: Long): AdminDeletionRequestResponse
}
