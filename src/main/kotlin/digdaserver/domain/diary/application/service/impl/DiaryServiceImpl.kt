package digdaserver.domain.diary.application.service.impl

import digdaserver.domain.comment.domain.entity.CommentTargetType
import digdaserver.domain.comment.domain.repository.CommentRepository
import digdaserver.domain.diary.application.service.DiaryService
import digdaserver.domain.diary.domain.entity.Diary
import digdaserver.domain.diary.domain.repository.DiaryRepository
import digdaserver.domain.diary.presentation.dto.req.CreateDiaryRequest
import digdaserver.domain.diary.presentation.dto.req.UpdateDiaryRequest
import digdaserver.domain.diary.presentation.dto.res.DiaryCalendarResponse
import digdaserver.domain.diary.presentation.dto.res.DiaryCommentResponse
import digdaserver.domain.diary.presentation.dto.res.DiaryDetailResponse
import digdaserver.domain.diary.presentation.dto.res.DiaryListResponse
import digdaserver.domain.diary.presentation.dto.res.DiaryResponse
import digdaserver.domain.diary.presentation.dto.res.DiarySummaryResponse
import digdaserver.domain.group_room.domain.repository.GroupRoomRepository
import digdaserver.domain.membership.domain.repository.MembershipRepository
import digdaserver.domain.notification.application.service.NotificationService
import digdaserver.domain.user.domain.repository.UserRepository
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@Service
@Transactional(readOnly = true)
class DiaryServiceImpl(
    private val diaryRepository: DiaryRepository,
    private val groupRoomRepository: GroupRoomRepository,
    private val membershipRepository: MembershipRepository,
    private val commentRepository: CommentRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService
) : DiaryService {

    override fun getDiaries(userId: UUID, groupRoomId: Long, month: YearMonth?, limit: Int, offset: Int): DiaryListResponse {
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        if (groupRoom.deletedAt != null) throw DigdaException(ErrorCode.GROUP_ROOM_ALREADY_DELETED)

        membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }

        val pageable = PageRequest.of(offset / limit, limit, Sort.by(Sort.Direction.DESC, "createdAt"))

        val page = if (month != null) {
            val startDate = month.atDay(1)
            val endDate = month.atEndOfMonth()
            diaryRepository.findAllByGroupRoomIdAndDateBetween(groupRoomId, startDate, endDate, pageable)
        } else {
            diaryRepository.findAllByGroupRoomId(groupRoomId, pageable)
        }

        return DiaryListResponse(
            diaries = page.content.map { diary ->
                val commentCount = commentRepository.countByTargetTypeAndTargetId(CommentTargetType.DIARY, diary.id)
                DiarySummaryResponse.from(diary, commentCount)
            },
            total = page.totalElements
        )
    }

    override fun getDiaryCalendar(userId: UUID, groupRoomId: Long, month: YearMonth): DiaryCalendarResponse {
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        if (groupRoom.deletedAt != null) throw DigdaException(ErrorCode.GROUP_ROOM_ALREADY_DELETED)

        membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }

        val startDate = month.atDay(1)
        val endDate = month.atEndOfMonth()
        val dates = diaryRepository.findDistinctDatesByGroupRoomIdAndMonth(groupRoomId, startDate, endDate)

        return DiaryCalendarResponse(dates = dates)
    }

    override fun getDiaryDetail(userId: UUID, groupRoomId: Long, diaryId: Long): DiaryDetailResponse {
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        if (groupRoom.deletedAt != null) throw DigdaException(ErrorCode.GROUP_ROOM_ALREADY_DELETED)

        membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }

        val diary = diaryRepository.findById(diaryId)
            .orElseThrow { DigdaException(ErrorCode.DIARY_NOT_FOUND) }

        val comments = commentRepository.findAllByTargetTypeAndTargetIdOrderByCreatedAtAsc(CommentTargetType.DIARY, diaryId)

        return DiaryDetailResponse(
            diary = DiaryResponse.from(diary),
            comments = comments.map { DiaryCommentResponse.from(it) }
        )
    }

    @Transactional
    override fun createDiary(userId: UUID, groupRoomId: Long, request: CreateDiaryRequest): DiaryResponse {
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        if (groupRoom.deletedAt != null) throw DigdaException(ErrorCode.GROUP_ROOM_ALREADY_DELETED)

        membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }

        if (request.date.isAfter(LocalDate.now())) throw DigdaException(ErrorCode.FUTURE_DATE_NOT_ALLOWED)
        validateWeather(request.weather)
        validateMood(request.mood)

        val user = userRepository.findById(userId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }

        val diary = diaryRepository.save(
            Diary(
                groupRoom = groupRoom,
                title = request.title,
                content = request.content,
                date = request.date,
                weather = request.weather,
                mood = request.mood,
                imageUrl = request.imageId,
                createdBy = user
            )
        )

        groupRoom.updateLastActivity()

        notificationService.notifyDiaryWritten(groupRoomId, diary.id, userId, diary.title)

        return DiaryResponse.from(diary)
    }

    @Transactional
    override fun updateDiary(userId: UUID, groupRoomId: Long, diaryId: Long, request: UpdateDiaryRequest): DiaryResponse {
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        if (groupRoom.deletedAt != null) throw DigdaException(ErrorCode.GROUP_ROOM_ALREADY_DELETED)

        val membership = membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }

        val diary = diaryRepository.findById(diaryId)
            .orElseThrow { DigdaException(ErrorCode.DIARY_NOT_FOUND) }

        if (diary.createdBy.id != userId && !membership.isOwner) {
            throw DigdaException(ErrorCode.FORBIDDEN)
        }

        request.date?.let {
            if (it.isAfter(LocalDate.now())) throw DigdaException(ErrorCode.FUTURE_DATE_NOT_ALLOWED)
        }
        request.weather?.let { validateWeather(it) }
        request.mood?.let { validateMood(it) }

        diary.update(
            title = request.title,
            content = request.content,
            date = request.date,
            weather = request.weather,
            mood = request.mood,
            imageUrl = request.imageId
        )

        groupRoom.updateLastActivity()

        return DiaryResponse.from(diary)
    }

    @Transactional
    override fun deleteDiary(userId: UUID, groupRoomId: Long, diaryId: Long) {
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        if (groupRoom.deletedAt != null) throw DigdaException(ErrorCode.GROUP_ROOM_ALREADY_DELETED)

        val membership = membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }

        val diary = diaryRepository.findById(diaryId)
            .orElseThrow { DigdaException(ErrorCode.DIARY_NOT_FOUND) }

        if (diary.createdBy.id != userId && !membership.isOwner) {
            throw DigdaException(ErrorCode.FORBIDDEN)
        }

        diaryRepository.delete(diary)
    }

    private fun validateWeather(weather: Int) {
        if (weather !in 0..3) throw DigdaException(ErrorCode.INVALID_WEATHER_VALUE)
    }

    private fun validateMood(mood: Int) {
        if (mood !in 0..3) throw DigdaException(ErrorCode.INVALID_MOOD_VALUE)
    }
}
