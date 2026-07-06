package digdaserver.admin.character.application.service

import digdaserver.admin.character.presentation.dto.req.AdminUpdateCharacterRequest
import digdaserver.admin.character.presentation.dto.res.AdminCharacterResponse
import digdaserver.admin.common.dto.res.AdminPageResponse

interface AdminCharacterService {

    fun search(
        keyword: String?,
        includeDeletedGroups: Boolean,
        page: Int,
        size: Int
    ): AdminPageResponse<AdminCharacterResponse>

    fun getDetail(groupRoomId: Long): AdminCharacterResponse

    fun update(groupRoomId: Long, request: AdminUpdateCharacterRequest): AdminCharacterResponse
}
