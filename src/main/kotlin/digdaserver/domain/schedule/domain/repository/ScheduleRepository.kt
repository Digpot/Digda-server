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
