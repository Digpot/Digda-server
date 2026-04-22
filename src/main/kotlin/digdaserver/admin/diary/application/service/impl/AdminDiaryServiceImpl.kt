package digdaserver.admin.diary.application.service.impl

import digdaserver.admin.common.dto.res.AdminPageResponse
import digdaserver.admin.diary.application.service.AdminDiaryService
import digdaserver.admin.diary.presentation.dto.res.AdminDiaryResponse
import digdaserver.admin.log.application.service.AdminActionLogService
import digdaserver.admin.log.domain.entity.AdminAction
import digdaserver.domain.diary.domain.repository.DiaryRepository
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminDiaryServiceImpl(
    private val diaryRepository: DiaryRepository,
    private val adminActionLogService: AdminActionLogService
) : AdminDiaryService {

    override fun search(keyword: String?, page: Int, size: Int): AdminPageResponse<AdminDiaryResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val result = diaryRepository.searchForAdmin(keyword, pageable)
        return AdminPageResponse.of(result, AdminDiaryResponse::from)
    }

    override fun getDetail(diaryId: Long): AdminDiaryResponse {
        val diary = diaryRepository.findById(diaryId)
            .orElseThrow { DigdaException(ErrorCode.DIARY_NOT_FOUND) }
        return AdminDiaryResponse.from(diary)
    }

    @Transactional
    override fun delete(diaryId: Long) {
        val diary = diaryRepository.findById(diaryId)
            .orElseThrow { DigdaException(ErrorCode.DIARY_NOT_FOUND) }
        val title = diary.title
        val groupRoomId = diary.groupRoom.id
        diaryRepository.delete(diary)

        adminActionLogService.record(
            actorId = currentActorId(),
            action = AdminAction.DELETE_DIARY,
            targetType = "DIARY",
            targetId = diaryId.toString(),
            detail = "groupRoomId=$groupRoomId, title=$title"
        )
    }

    private fun currentActorId(): UUID? {
        val principal = SecurityContextHolder.getContext().authentication?.principal as? String ?: return null
        return runCatching { UUID.fromString(principal) }.getOrNull()
    }
}
