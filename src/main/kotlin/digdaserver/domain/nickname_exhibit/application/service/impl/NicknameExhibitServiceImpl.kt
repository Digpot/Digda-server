package digdaserver.domain.nickname_exhibit.application.service.impl

import digdaserver.domain.nickname_exhibit.application.service.NicknameExhibitService
import digdaserver.domain.nickname_exhibit.domain.repository.NicknameExhibitAccessRepository
import digdaserver.domain.nickname_exhibit.domain.repository.NicknameExhibitRepository
import digdaserver.domain.nickname_exhibit.presentation.dto.res.NicknameExhibitResponse
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class NicknameExhibitServiceImpl(
    private val nicknameExhibitRepository: NicknameExhibitRepository,
    private val nicknameExhibitAccessRepository: NicknameExhibitAccessRepository
) : NicknameExhibitService {

    override fun hasAccess(userId: UUID): Boolean =
        nicknameExhibitAccessRepository.existsByUserId(userId)

    override fun list(userId: UUID): List<NicknameExhibitResponse> {
        // 버튼이 노출되지 않아도 직접 호출 가능하므로 목록 조회에서 한 번 더 권한을 확인한다.
        if (!nicknameExhibitAccessRepository.existsByUserId(userId)) {
            throw DigdaException(ErrorCode.EXHIBIT_ACCESS_DENIED)
        }
        return nicknameExhibitRepository.findAllByOrderBySortOrderAscIdAsc()
            .map(NicknameExhibitResponse::from)
    }
}
