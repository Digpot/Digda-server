package digdaserver.domain.schedule.application.service

import digdaserver.domain.schedule.presentation.dto.req.CreateScheduleRequest
import digdaserver.domain.schedule.presentation.dto.req.UpdateScheduleRequest
import digdaserver.domain.schedule.presentation.dto.res.ScheduleDetailResponse
import digdaserver.domain.schedule.presentation.dto.res.ScheduleListResponse
import digdaserver.domain.schedule.presentation.dto.res.ScheduleResponse
import java.time.LocalDate
import java.util.UUID

interface ScheduleService {

    fun getSchedules(userId: UUID, groupRoomId: Long, startDate: LocalDate, endDate: LocalDate): ScheduleListResponse

    fun getScheduleDetail(userId: UUID, groupRoomId: Long, scheduleId: Long): ScheduleDetailResponse

    fun createSchedule(userId: UUID, groupRoomId: Long, request: CreateScheduleRequest): ScheduleResponse

    fun updateSchedule(userId: UUID, groupRoomId: Long, scheduleId: Long, request: UpdateScheduleRequest): ScheduleResponse

    fun deleteSchedule(userId: UUID, groupRoomId: Long, scheduleId: Long)
}
