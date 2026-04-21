package digdaserver.domain.schedule.application.service.impl

import digdaserver.domain.comment.domain.entity.CommentTargetType
import digdaserver.domain.comment.domain.repository.CommentRepository
import digdaserver.domain.group_room.domain.repository.GroupRoomRepository
import digdaserver.domain.membership.domain.repository.MembershipRepository
import digdaserver.domain.notification.application.service.NotificationService
import digdaserver.domain.schedule.application.service.ScheduleService
import digdaserver.domain.schedule.domain.entity.Schedule
import digdaserver.domain.schedule.domain.entity.ScheduleParticipant
import digdaserver.domain.schedule.domain.repository.ScheduleRepository
import digdaserver.domain.schedule.presentation.dto.req.CreateScheduleRequest
import digdaserver.domain.schedule.presentation.dto.req.UpdateScheduleRequest
import digdaserver.domain.schedule.presentation.dto.res.CommentResponse
import digdaserver.domain.schedule.presentation.dto.res.ScheduleDetailResponse
import digdaserver.domain.schedule.presentation.dto.res.ScheduleListResponse
import digdaserver.domain.schedule.presentation.dto.res.ScheduleResponse
import digdaserver.domain.user.domain.repository.UserRepository
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
@Transactional(readOnly = true)
class ScheduleServiceImpl(
    private val scheduleRepository: ScheduleRepository,
    private val groupRoomRepository: GroupRoomRepository,
    private val membershipRepository: MembershipRepository,
    private val commentRepository: CommentRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService
) : ScheduleService {

    override fun getSchedules(userId: UUID, groupRoomId: Long, startDate: LocalDate, endDate: LocalDate): ScheduleListResponse {
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        if (groupRoom.deletedAt != null) throw DigdaException(ErrorCode.GROUP_ROOM_ALREADY_DELETED)

        membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }

        val schedules = scheduleRepository.findAllByGroupRoomIdAndDateRange(groupRoomId, startDate, endDate)

        return ScheduleListResponse(
            schedules = schedules.map { schedule ->
                val commentCount = commentRepository.countByTargetTypeAndTargetId(CommentTargetType.SCHEDULE, schedule.id)
                ScheduleResponse.from(schedule, commentCount)
            }
        )
    }

    override fun getScheduleDetail(userId: UUID, groupRoomId: Long, scheduleId: Long): ScheduleDetailResponse {
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        if (groupRoom.deletedAt != null) throw DigdaException(ErrorCode.GROUP_ROOM_ALREADY_DELETED)

        membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }

        val schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow { DigdaException(ErrorCode.SCHEDULE_NOT_FOUND) }

        val commentCount = commentRepository.countByTargetTypeAndTargetId(CommentTargetType.SCHEDULE, scheduleId)
        val comments = commentRepository.findAllByTargetTypeAndTargetIdOrderByCreatedAtAsc(CommentTargetType.SCHEDULE, scheduleId)

        return ScheduleDetailResponse(
            schedule = ScheduleResponse.from(schedule, commentCount),
            comments = comments.map { CommentResponse.from(it) }
        )
    }

    @Transactional
    override fun createSchedule(userId: UUID, groupRoomId: Long, request: CreateScheduleRequest): ScheduleResponse {
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        if (groupRoom.deletedAt != null) throw DigdaException(ErrorCode.GROUP_ROOM_ALREADY_DELETED)

        membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }

        validateDateRange(request.startDate, request.endDate, request.startTime, request.endTime, request.allDay)

        val user = userRepository.findById(userId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }

        val schedule = scheduleRepository.save(
            Schedule(
                groupRoom = groupRoom,
                title = request.title,
                color = request.color,
                startDate = request.startDate,
                endDate = request.endDate,
                startTime = if (request.allDay) null else request.startTime,
                endTime = if (request.allDay) null else request.endTime,
                allDay = request.allDay,
                createdBy = user
            )
        )

        request.participantIds?.let { ids ->
            addParticipants(schedule, groupRoomId, ids)
        }

        groupRoom.updateLastActivity()

        val participantIds = request.participantIds ?: emptyList()
        if (participantIds.isNotEmpty()) {
            notificationService.notifyScheduleCreated(
                groupRoomId = groupRoomId,
                scheduleId = schedule.id,
                creatorUserId = userId,
                scheduleTitle = schedule.title,
                participantUserIds = participantIds
            )
        }

        return ScheduleResponse.from(schedule, 0)
    }

    @Transactional
    override fun updateSchedule(userId: UUID, groupRoomId: Long, scheduleId: Long, request: UpdateScheduleRequest): ScheduleResponse {
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        if (groupRoom.deletedAt != null) throw DigdaException(ErrorCode.GROUP_ROOM_ALREADY_DELETED)

        val membership = membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }

        val schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow { DigdaException(ErrorCode.SCHEDULE_NOT_FOUND) }

        if (schedule.createdBy.id != userId && !membership.isOwner) {
            throw DigdaException(ErrorCode.FORBIDDEN)
        }

        val newStartDate = request.startDate ?: schedule.startDate
        val newEndDate = request.endDate ?: schedule.endDate
        val newAllDay = request.allDay ?: schedule.allDay
        val newStartTime = if (newAllDay) null else (request.startTime ?: schedule.startTime)
        val newEndTime = if (newAllDay) null else (request.endTime ?: schedule.endTime)

        validateDateRange(newStartDate, newEndDate, newStartTime, newEndTime, newAllDay)

        schedule.update(
            title = request.title,
            color = request.color,
            startDate = request.startDate,
            endDate = request.endDate,
            startTime = newStartTime,
            endTime = newEndTime,
            allDay = request.allDay
        )

        val addedParticipantIds = request.participantIds?.let { newIds ->
            val oldIds = schedule.participants.map { it.user.id }.toSet()
            schedule.clearParticipants()
            addParticipants(schedule, groupRoomId, newIds)
            newIds.filter { it !in oldIds }
        } ?: emptyList()

        groupRoom.updateLastActivity()

        if (addedParticipantIds.isNotEmpty()) {
            notificationService.notifyScheduleParticipantsAdded(
                groupRoomId = groupRoomId,
                scheduleId = schedule.id,
                actorUserId = userId,
                scheduleTitle = schedule.title,
                addedParticipantUserIds = addedParticipantIds
            )
        }

        val commentCount = commentRepository.countByTargetTypeAndTargetId(CommentTargetType.SCHEDULE, scheduleId)
        return ScheduleResponse.from(schedule, commentCount)
    }

    @Transactional
    override fun deleteSchedule(userId: UUID, groupRoomId: Long, scheduleId: Long) {
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        if (groupRoom.deletedAt != null) throw DigdaException(ErrorCode.GROUP_ROOM_ALREADY_DELETED)

        val membership = membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }

        val schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow { DigdaException(ErrorCode.SCHEDULE_NOT_FOUND) }

        if (schedule.createdBy.id != userId && !membership.isOwner) {
            throw DigdaException(ErrorCode.FORBIDDEN)
        }

        scheduleRepository.delete(schedule)
    }

    private fun validateDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
        startTime: java.time.LocalTime?,
        endTime: java.time.LocalTime?,
        allDay: Boolean
    ) {
        if (endDate.isBefore(startDate)) throw DigdaException(ErrorCode.END_DATE_BEFORE_START)

        if (!allDay && startDate == endDate && startTime != null && endTime != null) {
            if (endTime.isBefore(startTime)) throw DigdaException(ErrorCode.END_TIME_BEFORE_START)
        }
    }

    private fun addParticipants(schedule: Schedule, groupRoomId: Long, participantIds: List<UUID>) {
        participantIds.forEach { participantId ->
            val participantMembership = membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, participantId)
                .orElseThrow { DigdaException(ErrorCode.INVALID_PARTICIPANT) }

            schedule.addParticipant(
                ScheduleParticipant(
                    schedule = schedule,
                    user = participantMembership.user
                )
            )
        }
    }
}
