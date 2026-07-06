package digdaserver.admin.user.application.service

import digdaserver.admin.common.dto.res.AdminPageResponse
import digdaserver.admin.user.presentation.dto.res.AdminUserResponse
import digdaserver.domain.user.domain.entity.Role
import java.util.UUID

interface AdminUserService {

    fun search(keyword: String?, role: Role?, page: Int, size: Int): AdminPageResponse<AdminUserResponse>

    fun getDetail(userId: UUID): AdminUserResponse

    fun updateRole(userId: UUID, role: Role): AdminUserResponse

    fun updateRestriction(userId: UUID, restricted: Boolean): AdminUserResponse
}
