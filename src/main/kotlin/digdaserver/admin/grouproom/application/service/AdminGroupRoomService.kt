package digdaserver.admin.grouproom.application.service

import digdaserver.admin.common.dto.res.AdminPageResponse
import digdaserver.admin.grouproom.presentation.dto.req.GroupRoomAdminAction
import digdaserver.admin.grouproom.presentation.dto.res.AdminGroupRoomResponse

interface AdminGroupRoomService {

    fun search(keyword: String?, includeDeleted: Boolean, page: Int, size: Int): AdminPageResponse<AdminGroupRoomResponse>

    fun getDetail(groupRoomId: Long): AdminGroupRoomResponse

    fun changeStatus(groupRoomId: Long, action: GroupRoomAdminAction): AdminGroupRoomResponse
}
