package digdaserver.domain.character.domain.repository

import digdaserver.domain.character.domain.entity.GroupCharacter
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface GroupCharacterRepository : JpaRepository<GroupCharacter, Long> {
    fun findByGroupRoomId(groupRoomId: Long): GroupCharacter?

    /**
     * 어드민 페이지네이션 검색.
     * - [keyword]: 그룹방 이름 또는 방장 이름 LIKE (대소문자 무시, null/빈 = 무필터)
     * - [includeDeletedGroups]: false 면 deletedAt IS NULL 인 그룹방만
     */
    @Query(
        """
        SELECT c FROM GroupCharacter c
        JOIN c.groupRoom g
        WHERE (:keyword IS NULL OR :keyword = ''
            OR LOWER(g.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(g.owner.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:includeDeletedGroups = true OR g.deletedAt IS NULL)
        """
    )
    fun searchForAdmin(
        @Param("keyword") keyword: String?,
        @Param("includeDeletedGroups") includeDeletedGroups: Boolean,
        pageable: Pageable
    ): Page<GroupCharacter>
}
