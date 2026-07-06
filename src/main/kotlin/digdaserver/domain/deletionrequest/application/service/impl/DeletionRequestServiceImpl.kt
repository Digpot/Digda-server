package digdaserver.domain.deletionrequest.application.service.impl

import digdaserver.domain.deletionrequest.application.service.DeletionRequestService
import digdaserver.domain.deletionrequest.domain.entity.DeletionRequest
import digdaserver.domain.deletionrequest.domain.entity.DeletionRequestType
import digdaserver.domain.deletionrequest.domain.repository.DeletionRequestRepository
import digdaserver.domain.deletionrequest.presentation.dto.req.CreateAccountDeletionRequest
import digdaserver.domain.deletionrequest.presentation.dto.req.CreateDataDeletionRequest
import digdaserver.domain.deletionrequest.presentation.dto.res.DeletionRequestResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class DeletionRequestServiceImpl(
    private val deletionRequestRepository: DeletionRequestRepository
) : DeletionRequestService {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun createAccountDeletion(request: CreateAccountDeletionRequest): DeletionRequestResponse {
        val saved = deletionRequestRepository.save(
            DeletionRequest(
                type = DeletionRequestType.ACCOUNT,
                email = request.email.trim()
            )
        )
        log.info(
            "action=계정 삭제 요청 접수(비로그인), deletionRequestId={}, email={}",
            saved.id,
            saved.email
        )
        return DeletionRequestResponse.from(saved)
    }

    @Transactional
    override fun createDataDeletion(request: CreateDataDeletionRequest): DeletionRequestResponse {
        val saved = deletionRequestRepository.save(
            DeletionRequest(
                type = DeletionRequestType.DATA,
                email = request.email.trim(),
                groupRoomName = request.groupRoomName.trim(),
                content = request.content.trim()
            )
        )
        log.info(
            "action=데이터 삭제 요청 접수(비로그인), deletionRequestId={}, email={}, groupRoomName={}",
            saved.id,
            saved.email,
            saved.groupRoomName
        )
        return DeletionRequestResponse.from(saved)
    }
}
