package digdaserver.domain.schedule.domain.repository

import digdaserver.domain.schedule.domain.entity.Schedule
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface ScheduleRepository : JpaRepository<Schedule, Long> {

    @Query("SELECT s FROM Schedule s WHERE s.groupRoom.id = :groupRoomId AND s.startDate <= :endDate AND s.endDate >= :startDate ORDER BY s.startDate ASC")
    fun findAllByGroupRoomIdAndDateRange(groupRoomId: Long, startDate: LocalDate, endDate: LocalDate): List<Schedule>

    /**
     * 시작일이 [date] 이고 삭제되지 않은 그룹방에 속한 일정 목록 — 리마인더 발송 대상 조회.
     * 참가자가 없는 일정도 포함하도록 LEFT JOIN FETCH 로 participants 를 함께 로딩한다.
     */
    @Query(
        """
        SELECT DISTINCT s FROM Schedule s
        LEFT JOIN FETCH s.participants
        WHERE s.startDate = :date AND s.groupRoom.deletedAt IS NULL
        """
    )
    fun findAllForReminder(@Param("date") date: LocalDate): List<Schedule>

    @Query(
        """
        SELECT s FROM Schedule s
        WHERE (:keyword IS NULL OR :keyword = ''
            OR LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """
    )
    fun searchForAdmin(
        @Param("keyword") keyword: String?,
        pageable: Pageable
    ): Page<Schedule>
}
