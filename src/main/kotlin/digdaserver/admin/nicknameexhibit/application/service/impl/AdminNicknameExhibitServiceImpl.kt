package digdaserver.admin.nicknameexhibit.application.service.impl

import digdaserver.admin.common.dto.res.AdminPageResponse
import digdaserver.admin.nicknameexhibit.application.service.AdminNicknameExhibitService
import digdaserver.admin.nicknameexhibit.presentation.dto.req.CreateNicknameExhibitRequest
import digdaserver.admin.nicknameexhibit.presentation.dto.req.UpdateNicknameExhibitRequest
import digdaserver.admin.nicknameexhibit.presentation.dto.res.AdminExhibitAccessResponse
import digdaserver.admin.nicknameexhibit.presentation.dto.res.AdminNicknameExhibitResponse
import digdaserver.domain.nickname_exhibit.domain.entity.NicknameExhibit
import digdaserver.domain.nickname_exhibit.domain.entity.NicknameExhibitAccess
import digdaserver.domain.nickname_exhibit.domain.repository.NicknameExhibitAccessRepository
import digdaserver.domain.nickname_exhibit.domain.repository.NicknameExhibitRepository
import digdaserver.domain.user.domain.repository.UserRepository
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class AdminNicknameExhibitServiceImpl(
    private val nicknameExhibitRepository: NicknameExhibitRepository,
    private val nicknameExhibitAccessRepository: NicknameExhibitAccessRepository,
    private val userRepository: UserRepository
) : AdminNicknameExhibitService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun search(
        keyword: String?,
        page: Int,
        size: Int
    ): AdminPageResponse<AdminNicknameExhibitResponse> {
        val pageable = PageRequest.of(
            page.coerceAtLeast(0),
            size.coerceIn(1, 100),
            Sort.by(Sort.Direction.ASC, "sortOrder").and(Sort.by(Sort.Direction.ASC, "id"))
        )
        val result = nicknameExhibitRepository.searchForAdmin(
            keyword?.takeIf { it.isNotBlank() },
            pageable
        )
        return AdminPageResponse.of(result, AdminNicknameExhibitResponse::from)
    }

    @Transactional
    override fun create(request: CreateNicknameExhibitRequest): AdminNicknameExhibitResponse {
        val saved = nicknameExhibitRepository.save(
            NicknameExhibit(
                nickname = request.nickname,
                imageUrl = request.imageUrl?.takeIf { it.isNotBlank() },
                history = request.history,
                sortOrder = request.sortOrder
            )
        )
        log.info("action=admin_exhibit_create, id={}, nickname={}", saved.id, saved.nickname)
        return AdminNicknameExhibitResponse.from(saved)
    }

    @Transactional
    override fun update(id: Long, request: UpdateNicknameExhibitRequest): AdminNicknameExhibitResponse {
        val exhibit = nicknameExhibitRepository.findById(id)
            .orElseThrow { DigdaException(ErrorCode.EXHIBIT_NOT_FOUND) }
        exhibit.update(
            nickname = request.nickname,
            imageUrl = request.imageUrl,
            history = request.history,
            sortOrder = request.sortOrder
        )
        log.info("action=admin_exhibit_update, id={}", id)
        return AdminNicknameExhibitResponse.from(exhibit)
    }

    @Transactional
    override fun delete(id: Long) {
        val exhibit = nicknameExhibitRepository.findById(id)
            .orElseThrow { DigdaException(ErrorCode.EXHIBIT_NOT_FOUND) }
        nicknameExhibitRepository.delete(exhibit)
        log.info("action=admin_exhibit_delete, id={}", id)
    }

    override fun searchAccess(
        keyword: String?,
        page: Int,
        size: Int
    ): AdminPageResponse<AdminExhibitAccessResponse> {
        val pageable = PageRequest.of(
            page.coerceAtLeast(0),
            size.coerceIn(1, 100),
            Sort.by(Sort.Direction.DESC, "createdAt")
        )
        val result = nicknameExhibitAccessRepository.searchForAdmin(
            keyword?.takeIf { it.isNotBlank() },
            pageable
        )
        return AdminPageResponse.of(result, AdminExhibitAccessResponse::from)
    }

    @Transactional
    override fun addAccess(userId: UUID): AdminExhibitAccessResponse {
        // 멱등 — 이미 허용된 사용자면 기존 행을 그대로 반환.
        nicknameExhibitAccessRepository.findByUserId(userId)?.let {
            return AdminExhibitAccessResponse.from(it)
        }
        val user = userRepository.findById(userId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }
        val saved = nicknameExhibitAccessRepository.save(NicknameExhibitAccess(user = user))
        log.info("action=admin_exhibit_access_add, userId={}", userId)
        return AdminExhibitAccessResponse.from(saved)
    }

    @Transactional
    override fun removeAccess(userId: UUID) {
        nicknameExhibitAccessRepository.deleteByUserId(userId)
        log.info("action=admin_exhibit_access_remove, userId={}", userId)
    }
}
