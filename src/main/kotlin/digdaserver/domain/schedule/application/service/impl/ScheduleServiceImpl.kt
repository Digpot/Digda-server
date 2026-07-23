package digdaserver.domain.schedule.application.service.impl

import digdaserver.domain.block.application.service.ContentVisibilityService
import digdaserver.domain.comment.domain.entity.CommentTargetType
import digdaserver.domain.comment.domain.repository.CommentRepository
import digdaserver.domain.group_room.domain.repository.GroupRoomRepository
import digdaserver.domain.log.application.service.UserActionLogService
import digdaserver.domain.log.domain.entity.UserAction
import digdaserver.domain.membership.domain.repository.MembershipRepository
import digdaserver.domain.notification.application.service.NotificationService
import digdaserver.domain.schedule.application.service.ScheduleService
import digdaserver.domain.schedule.domain.entity.Schedule
import digdaserver.domain.schedule.domain.entity.ScheduleParticipant
import digdaserver.domain.schedule.domain.repository.ScheduleRepository
import digdaserver.domain.schedule.presentation.dto.req.CopyScheduleRequest
import digdaserver.domain.schedule.presentation.dto.req.CreateScheduleRequest
import digdaserver.domain.schedule.presentation.dto.req.UpdateScheduleRequest
import digdaserver.domain.schedule.presentation.dto.res.CommentResponse
import digdaserver.domain.schedule.presentation.dto.res.ScheduleDetailResponse
import digdaserver.domain.schedule.presentation.dto.res.ScheduleListResponse
import digdaserver.domain.schedule.presentation.dto.res.ScheduleResponse
import digdaserver.domain.user.domain.repository.UserRepository
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
@Transactional(readOnly = true)
class ScheduleServiceImpl(
    private val scheduleRepository: ScheduleRepository,
    private val groupRoomRepository: GroupRoomRepository,
    private val membershipRepository: MembershipRepository,
    private val commentRepository: CommentRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService,
    private val userActionLogService: UserActionLogService,
    private val contentVisibilityService: ContentVisibilityService
) : ScheduleService {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        // 한 번의 복사 요청으로 만들 수 있는 최대 날짜 수 (한 달치).
        const val MAX_COPY_DATES = 31
    }

    override fun getSchedules(userId: UUID, groupRoomId: Long, startDate: LocalDate, endDate: LocalDate): ScheduleListResponse {
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        if (groupRoom.deletedAt != null) throw DigdaException(ErrorCode.GROUP_ROOM_ALREADY_DELETED)

        membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }

        val allSchedules = scheduleRepository.findAllByGroupRoomIdAndDateRange(groupRoomId, startDate, endDate)

        // 차단/신고 숨김 — 일정은 슬롯 제약이 없어 캘린더에서 아예 제외한다(작성자 차단 또는 개별 숨김).
        val visibility = contentVisibilityService.forViewer(userId)
        val schedules = allSchedules.filter {
            visibility.scheduleHiddenReason(it.id, it.createdBy.id) == null
        }

        val scheduleIds = schedules.map { it.id }
        val commentCountMap: Map<Long, Int> = if (scheduleIds.isNotEmpty()) {
            commentRepository.countByTargetTypeAndTargetIdIn(CommentTargetType.SCHEDULE, scheduleIds)
                .associate { row -> (row[0] as Long) to (row[1] as Long).toInt() }
        } else {
            emptyMap()
        }

        return ScheduleListResponse(
            schedules = schedules.map { schedule ->
                val commentCount = commentCountMap[schedule.id] ?: 0
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

        // 차단/신고 숨김 — 일정 본문(직접 접근 방어)과 차단 사용자 댓글을 비워 내려보낸다.
        val visibility = contentVisibilityService.forViewer(userId)
        val scheduleResponse = ScheduleResponse.from(schedule, commentCount).let { resp ->
            val reason = visibility.scheduleHiddenReason(schedule.id, schedule.createdBy.id)
            if (reason != null) resp.asHidden(reason) else resp
        }

        return ScheduleDetailResponse(
            schedule = scheduleResponse,
            comments = comments.map { comment ->
                val resp = CommentResponse.from(comment)
                val reason = visibility.commentHiddenReason(comment.id, comment.createdBy.id)
                if (reason != null) resp.asHidden(reason) else resp
            }
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

        if (user.restricted) throw DigdaException(ErrorCode.USER_RESTRICTED)

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

        userActionLogService.record(
            actorId = userId,
            action = UserAction.CREATE_SCHEDULE,
            targetType = "SCHEDULE",
            targetId = schedule.id.toString(),
            detail = "groupRoomId=$groupRoomId, title=${schedule.title}"
        )

        return ScheduleResponse.from(schedule, 0)
    }

    @Transactional
    override fun copySchedule(userId: UUID, groupRoomId: Long, scheduleId: Long, request: CopyScheduleRequest): ScheduleListResponse {
        log.info("copySchedule: userId={}, groupRoomId={}, scheduleId={}, dates={}", userId, groupRoomId, scheduleId, request.dates.size)

        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        if (groupRoom.deletedAt != null) throw DigdaException(ErrorCode.GROUP_ROOM_ALREADY_DELETED)

        membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }

        val dates = request.dates.distinct()
        if (dates.isEmpty()) throw DigdaException(ErrorCode.SCHEDULE_COPY_DATES_EMPTY)
        if (dates.size > MAX_COPY_DATES) throw DigdaException(ErrorCode.SCHEDULE_COPY_LIMIT_EXCEEDED)

        val source = scheduleRepository.findById(scheduleId)
            .orElseThrow { DigdaException(ErrorCode.SCHEDULE_NOT_FOUND) }

        if (source.groupRoom.id != groupRoomId) throw DigdaException(ErrorCode.SCHEDULE_NOT_FOUND)

        // 차단/신고로 내게 숨겨진 일정은 복사 원본으로 쓸 수 없다 (직접 접근 방어).
        val visibility = contentVisibilityService.forViewer(userId)
        if (visibility.scheduleHiddenReason(source.id, source.createdBy.id) != null) {
            throw DigdaException(ErrorCode.SCHEDULE_NOT_FOUND)
        }

        val user = userRepository.findById(userId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }

        if (user.restricted) throw DigdaException(ErrorCode.USER_RESTRICTED)

        // 기간 일정은 길이를 유지한 채 선택 날짜를 시작일로 복사한다.
        val durationDays = ChronoUnit.DAYS.between(source.startDate, source.endDate)
        val participantIds = source.participants.map { it.user.id }

        val copies = dates.sorted().map { date ->
            val copy = scheduleRepository.save(
                Schedule(
                    groupRoom = groupRoom,
                    title = source.title,
                    color = source.color,
                    startDate = date,
                    endDate = date.plusDays(durationDays),
                    startTime = source.startTime,
                    endTime = source.endTime,
                    allDay = source.allDay,
                    createdBy = user
                )
            )
            addParticipants(copy, groupRoomId, participantIds)
            copy
        }

        groupRoom.updateLastActivity()

        // 복사는 최대 31건이 한 번에 생성되므로, 참여자 알림은 건별로 보내지 않는다
        // (생성 알림 31개 스팸 방지). 액션 로그도 요청 단위로 1건만 남긴다.
        userActionLogService.record(
            actorId = userId,
            action = UserAction.CREATE_SCHEDULE,
            targetType = "SCHEDULE",
            targetId = source.id.toString(),
            detail = "groupRoomId=$groupRoomId, title=${source.title}, copiedTo=${dates.size} dates"
        )

        return ScheduleListResponse(
            schedules = copies.map { ScheduleResponse.from(it, 0) }
        )
    }

    @Transactional
    override fun updateSchedule(userId: UUID, groupRoomId: Long, scheduleId: Long, request: UpdateScheduleRequest): ScheduleResponse {
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        if (groupRoom.deletedAt != null) throw DigdaException(ErrorCode.GROUP_ROOM_ALREADY_DELETED)

        // 일정은 그룹 멤버 누구나 수정 가능 (작성자/방장 제한 없음 — 일기와 반대).
        membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }

        val schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow { DigdaException(ErrorCode.SCHEDULE_NOT_FOUND) }

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
            val oldIdSet = schedule.participants.map { it.user.id }.toSet()
            val newIdSet = newIds.toSet()

            // 제거: 새 목록에 없는 기존 참여자만 삭제 (orphanRemoval 트리거)
            val iterator = schedule.participants.iterator()
            while (iterator.hasNext()) {
                if (iterator.next().user.id !in newIdSet) iterator.remove()
            }

            // 추가: 기존에 없는 참여자만 INSERT — clear/re-insert 패턴 제거로 duplicate key 방지
            val toAdd = newIds.filter { it !in oldIdSet }
            addParticipants(schedule, groupRoomId, toAdd)

            toAdd
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

        // 일정은 그룹 멤버 누구나 삭제 가능 (작성자/방장 제한 없음 — 일기와 반대).
        membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }

        val schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow { DigdaException(ErrorCode.SCHEDULE_NOT_FOUND) }

        val title = schedule.title
        scheduleRepository.delete(schedule)

        userActionLogService.record(
            actorId = userId,
            action = UserAction.DELETE_SCHEDULE,
            targetType = "SCHEDULE",
            targetId = scheduleId.toString(),
            detail = "groupRoomId=$groupRoomId, title=$title"
        )
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
