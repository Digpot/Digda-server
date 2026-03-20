package digdaserver.domain.schedule.domain.repository

import digdaserver.domain.schedule.domain.entity.ScheduleParticipant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ScheduleParticipantRepository : JpaRepository<ScheduleParticipant, Long> {

    fun findAllByScheduleId(scheduleId: Long): List<ScheduleParticipant>

    fun deleteAllByScheduleId(scheduleId: Long)
}
