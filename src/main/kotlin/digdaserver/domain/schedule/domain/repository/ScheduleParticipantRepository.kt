package digdaserver.domain.schedule.domain.repository

import digdaserver.domain.schedule.domain.entity.ScheduleParticipant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ScheduleParticipantRepository : JpaRepository<ScheduleParticipant, Long> {

    fun findAllByScheduleId(scheduleId: Long): List<ScheduleParticipant>

    fun deleteAllByScheduleId(scheduleId: Long)

    @Modifying
    @Query("DELETE FROM ScheduleParticipant sp WHERE sp.user.id = :userId")
    fun deleteAllByUserId(@Param("userId") userId: UUID)

    @Modifying
    @Query("DELETE FROM ScheduleParticipant sp WHERE sp.schedule.createdBy.id = :userId")
    fun deleteAllByScheduleCreatedById(@Param("userId") userId: UUID)
}
