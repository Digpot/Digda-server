package digdaserver.admin.title.application.service

import digdaserver.admin.title.presentation.dto.res.AdminUserTitleResponse
import digdaserver.domain.title.presentation.dto.res.TitleCatalogResponse
import java.util.UUID

interface AdminTitleService {

    /** 칭호 카탈로그 전체(부여 대상 목록). */
    fun catalog(): List<TitleCatalogResponse>

    /** 특정 사용자가 보유한 칭호 목록. */
    fun userTitles(userId: UUID): List<AdminUserTitleResponse>

    /** 사용자에게 칭호 부여(멱등). 카탈로그에 없는 코드면 예외. */
    fun grant(userId: UUID, code: String): List<AdminUserTitleResponse>

    /** 사용자 칭호 회수. */
    fun revoke(userId: UUID, code: String): List<AdminUserTitleResponse>
}
