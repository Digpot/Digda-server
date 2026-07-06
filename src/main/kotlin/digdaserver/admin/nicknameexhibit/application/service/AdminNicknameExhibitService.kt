package digdaserver.admin.nicknameexhibit.application.service

import digdaserver.admin.common.dto.res.AdminPageResponse
import digdaserver.admin.nicknameexhibit.presentation.dto.req.CreateNicknameExhibitRequest
import digdaserver.admin.nicknameexhibit.presentation.dto.req.UpdateNicknameExhibitRequest
import digdaserver.admin.nicknameexhibit.presentation.dto.res.AdminExhibitAccessResponse
import digdaserver.admin.nicknameexhibit.presentation.dto.res.AdminNicknameExhibitResponse
import java.util.UUID

interface AdminNicknameExhibitService {

    // ── 콘텐츠(별명 카드) CRUD ──
    fun search(keyword: String?, page: Int, size: Int): AdminPageResponse<AdminNicknameExhibitResponse>
    fun create(request: CreateNicknameExhibitRequest): AdminNicknameExhibitResponse
    fun update(id: Long, request: UpdateNicknameExhibitRequest): AdminNicknameExhibitResponse
    fun delete(id: Long)

    // ── 접근 허용 사용자 관리 ──
    fun searchAccess(keyword: String?, page: Int, size: Int): AdminPageResponse<AdminExhibitAccessResponse>
    fun addAccess(userId: UUID): AdminExhibitAccessResponse
    fun removeAccess(userId: UUID)
}
