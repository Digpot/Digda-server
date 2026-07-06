package digdaserver.domain.title.domain.repository

import digdaserver.domain.title.domain.entity.TitleCatalogEntry
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TitleCatalogRepository : JpaRepository<TitleCatalogEntry, Long> {

    fun findByCode(code: String): TitleCatalogEntry?

    fun findAllByOrderBySortOrderAscIdAsc(): List<TitleCatalogEntry>

    fun findAllByConditionType(conditionType: String): List<TitleCatalogEntry>

    fun existsByCode(code: String): Boolean
}
