package digdaserver.admin.grouproom.application.service.impl

import digdaserver.admin.common.dto.res.AdminPageResponse
import digdaserver.admin.grouproom.application.service.AdminGroupRoomService
import digdaserver.admin.grouproom.presentation.dto.req.GroupRoomAdminAction
import digdaserver.admin.grouproom.presentation.dto.res.AdminGroupRoomResponse
import digdaserver.domain.group_room.domain.repository.GroupRoomRepository
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminGroupRoomServiceImpl(
    private val groupRoomRepository: GroupRoomRepository
) : AdminGroupRoomService {

    override fun search(
        keyword: String?,
        includeDeleted: Boolean,
        page: Int,
        size: Int
    ): AdminPageResponse<AdminGroupRoomResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val result = groupRoomRepository.searchForAdmin(keyword, includeDeleted, pageable)
        return AdminPageResponse.of(result, AdminGroupRoomResponse::from)
    }

    override fun getDetail(groupRoomId: Long): AdminGroupRoomResponse {
        val room = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }
        return AdminGroupRoomResponse.from(room)
    }

    @Transactional
    override fun changeStatus(groupRoomId: Long, action: GroupRoomAdminAction): AdminGroupRoomResponse {
        val room = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        when (action) {
            GroupRoomAdminAction.RECOVER -> room.recover()
            GroupRoomAdminAction.SCHEDULE_DELETE -> room.scheduleDelete()
            GroupRoomAdminAction.HARD_DELETE -> room.markDeleted()
        }

        return AdminGroupRoomResponse.from(room)
    }
}
