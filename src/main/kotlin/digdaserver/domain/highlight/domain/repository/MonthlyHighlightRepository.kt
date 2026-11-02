package digdaserver.domain.highlight.domain.repository

import digdaserver.domain.highlight.domain.entity.MonthlyHighlight
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface MonthlyHighlightRepository : JpaRepository<MonthlyHighlight, Long> {

    fun existsByGroupRoomIdAndTargetMonth(groupRoomId: Long, targetMonth: String): Boolean

    fun findByGroupRoomIdAndTargetMonth(groupRoomId: Long, targetMonth: String): MonthlyHighlight?

    fun findAllByGroupRoomIdOrderByTargetMonthDesc(groupRoomId: Long): List<MonthlyHighlight>

    /**
     * 기간 내 일기가 있고, 아직 해당 월 하이라이트가 없는 (삭제되지 않은) 그룹 id.
     * 스케줄러가 처리할 대상을 좁히는 용도 — 일기가 없는 그룹은 아예 대상에서 빠진다.
     */
    @Query(
        "SELECT DISTINCT d.groupRoom.id FROM Diary d " +
            "WHERE d.date BETWEEN :startDate AND :endDate AND d.groupRoom.deletedAt IS NULL " +
            "AND NOT EXISTS (SELECT h.id FROM MonthlyHighlight h " +
            "WHERE h.groupRoomId = d.groupRoom.id AND h.targetMonth = :targetMonth)"
    )
    fun findGroupIdsPendingHighlight(
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
        @Param("targetMonth") targetMonth: String
    ): List<Long>
}
