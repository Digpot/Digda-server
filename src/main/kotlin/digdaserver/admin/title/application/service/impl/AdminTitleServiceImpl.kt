package digdaserver.admin.title.application.service.impl

import digdaserver.admin.title.application.service.AdminTitleService
import digdaserver.admin.title.presentation.dto.res.AdminUserTitleResponse
import digdaserver.domain.title.domain.entity.TitleCatalogEntry
import digdaserver.domain.title.domain.entity.UserTitle
import digdaserver.domain.title.domain.repository.TitleCatalogRepository
import digdaserver.domain.title.domain.repository.UserTitleRepository
import digdaserver.domain.title.presentation.dto.res.TitleCatalogResponse
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class AdminTitleServiceImpl(
    private val userTitleRepository: UserTitleRepository,
    private val titleCatalogRepository: TitleCatalogRepository
) : AdminTitleService {

    override fun catalog(): List<TitleCatalogResponse> =
        titleCatalogRepository.findAllByOrderBySortOrderAscIdAsc()
            .map(TitleCatalogResponse::from)

    override fun userTitles(userId: UUID): List<AdminUserTitleResponse> {
        val catalog = titleCatalogRepository.findAll().associateBy { it.code }
        return userTitleRepository.findAllByUserIdOrderByEarnedAtDesc(userId)
            .map { toResponse(it, catalog[it.code]) }
    }

    @Transactional
    override fun grant(userId: UUID, code: String): List<AdminUserTitleResponse> {
        val def = titleCatalogRepository.findByCode(code)
            ?: throw DigdaException(ErrorCode.INVALID_PARAMETER)
        if (!userTitleRepository.existsByUserIdAndCode(userId, def.code)) {
            userTitleRepository.save(UserTitle(userId = userId, code = def.code))
        }
        return userTitles(userId)
    }

    @Transactional
    override fun revoke(userId: UUID, code: String): List<AdminUserTitleResponse> {
        userTitleRepository.deleteByUserIdAndCode(userId, code)
        return userTitles(userId)
    }

    private fun toResponse(t: UserTitle, def: TitleCatalogEntry?): AdminUserTitleResponse =
        AdminUserTitleResponse.of(
            t = t,
            name = def?.name ?: t.code,
            category = def?.category ?: "unknown",
            accentColor = def?.accentColor ?: "#999999"
        )
}
