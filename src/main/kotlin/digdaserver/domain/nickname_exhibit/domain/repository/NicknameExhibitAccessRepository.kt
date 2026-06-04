package digdaserver.domain.nickname_exhibit.domain.repository

import digdaserver.domain.nickname_exhibit.domain.entity.NicknameExhibitAccess
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface NicknameExhibitAccessRepository : JpaRepository<NicknameExhibitAccess, Long> {

    fun existsByUserId(userId: UUID): Boolean

    fun findByUserId(userId: UUID): NicknameExhibitAccess?

    fun deleteByUserId(userId: UUID)

    /** 어드민 허용 목록 — 사용자 join 으로 이름/이메일 키워드 검색. */
    @Query(
        """
        SELECT a FROM NicknameExhibitAccess a
        JOIN a.user u
        WHERE (:keyword IS NULL OR :keyword = ''
            OR LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """
    )
    fun searchForAdmin(
        @Param("keyword") keyword: String?,
        pageable: Pageable
    ): Page<NicknameExhibitAccess>
}
