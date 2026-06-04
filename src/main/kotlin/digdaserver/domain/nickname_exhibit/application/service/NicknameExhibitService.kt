package digdaserver.domain.nickname_exhibit.application.service

import digdaserver.domain.nickname_exhibit.presentation.dto.res.NicknameExhibitResponse
import java.util.UUID

interface NicknameExhibitService {

    /** 해당 사용자가 전시관에 접근 가능한지. */
    fun hasAccess(userId: UUID): Boolean

    /** 전시관 카드 목록. 접근 권한 없으면 EXHIBIT_ACCESS_DENIED. */
    fun list(userId: UUID): List<NicknameExhibitResponse>
}
