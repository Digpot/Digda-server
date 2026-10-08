package digdaserver.domain.character.domain.repository

import digdaserver.domain.character.domain.entity.GroupCharacter
import digdaserver.domain.group_room.domain.entity.GroupRoom
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

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

    /**
     * 이벤트 코인 일괄 지급 — 살아있는 그룹의 모찌 전부에 [amount] 를 더한다. 반환값은 갱신된 행 수.
     *
     * 그룹 수만큼 엔티티를 로드하면 지급이 초 단위로 늘어져서 벌크 UPDATE 로 처리한다.
     * 벌크 UPDATE 는 영속성 컨텍스트를 우회하므로 [org.springframework.data.jpa.repository.Modifying]
     * 의 flush/clear 로 1차 캐시의 낡은 코인 값을 정리하고, Auditing 이 타지 않는 updatedAt 은
     * 쿼리에서 직접 갱신한다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE GroupCharacter c
        SET c.coin = c.coin + :amount, c.updatedAt = :now
        WHERE c.groupRoom.id IN (SELECT g.id FROM GroupRoom g WHERE g.deletedAt IS NULL)
        """
    )
    fun addCoinToActiveGroups(
        @Param("amount") amount: Int,
        @Param("now") now: LocalDateTime
    ): Int

    /**
     * 아직 모찌 행이 없는(= 캐릭터 화면에 한 번도 들어오지 않은) 살아있는 그룹들.
     *
     * 모찌 행은 캐릭터 화면 첫 진입 때 lazy 생성이라, 이 그룹들은 벌크 UPDATE 대상에서 빠져
     * 이벤트 코인을 못 받는다. 일괄 지급 전에 빈 행을 만들어 주기 위해 조회한다.
     */
    @Query(
        """
        SELECT g FROM GroupRoom g
        WHERE g.deletedAt IS NULL
          AND NOT EXISTS (SELECT c FROM GroupCharacter c WHERE c.groupRoom = g)
        """
    )
    fun findActiveGroupRoomsWithoutCharacter(): List<GroupRoom>
}
