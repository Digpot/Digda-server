package digdaserver.domain.schedule.domain.repository

import digdaserver.domain.schedule.domain.entity.Schedule
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface ScheduleRepository : JpaRepository<Schedule, Long> {

    @Query("SELECT s FROM Schedule s WHERE s.group.id = :groupId AND s.startDate <= :endDate AND s.endDate >= :startDate ORDER BY s.startDate ASC")
    fun findAllByGroupIdAndDateRange(groupId: Long, startDate: LocalDate, endDate: LocalDate): List<Schedule>
}
