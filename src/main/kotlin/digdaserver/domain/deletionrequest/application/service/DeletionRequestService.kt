package digdaserver.domain.deletionrequest.application.service

import digdaserver.domain.deletionrequest.presentation.dto.req.CreateAccountDeletionRequest
import digdaserver.domain.deletionrequest.presentation.dto.req.CreateDataDeletionRequest
import digdaserver.domain.deletionrequest.presentation.dto.res.DeletionRequestResponse

interface DeletionRequestService {

    /** 계정 삭제 요청 접수. */
    fun createAccountDeletion(request: CreateAccountDeletionRequest): DeletionRequestResponse

    /** 데이터 삭제 요청 접수. */
    fun createDataDeletion(request: CreateDataDeletionRequest): DeletionRequestResponse
}
