package digdaserver.domain.nickname_exhibit.domain.repository

import digdaserver.domain.nickname_exhibit.domain.entity.NicknameExhibit
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface NicknameExhibitRepository : JpaRepository<NicknameExhibit, Long> {

    /** 앱 노출용 — 정렬 순서대로 전체 목록. */
    fun findAllByOrderBySortOrderAscIdAsc(): List<NicknameExhibit>

    /** 어드민 페이지네이션 검색 — 별명 LIKE (null/빈 = 무필터). */
    @Query(
        """
        SELECT e FROM NicknameExhibit e
        WHERE (:keyword IS NULL OR :keyword = ''
            OR LOWER(e.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """
    )
    fun searchForAdmin(
        @Param("keyword") keyword: String?,
        pageable: Pageable
    ): Page<NicknameExhibit>
}
